"""Ejecutor de programas de usuario para Ghost.

Este módulo corre dentro del intérprete CPython real que aporta Chaquopy.
Se comunica con la app Kotlin mediante un objeto "bridge" que expone:

    bridge.write(texto: str, es_error: bool)   -> salida estándar / de error
    bridge.readLine(prompt: str) -> str | None -> entrada estándar (bloquea hasta que
                                                   el usuario escribe en la consola;
                                                   devuelve None en EOF / al detener)

sys.stdout, sys.stderr, sys.stdin e input() se redirigen a ese puente, de modo que
print() e input() funcionan de forma interactiva con la consola de la app.
"""
import builtins
import ctypes
import io
import os
import runpy
import sys
import threading
import traceback

_state = {"thread_id": None}


class _Writer(io.TextIOBase):
    """Sustituto de sys.stdout / sys.stderr que reenvía todo a la consola de Ghost."""

    def __init__(self, bridge, is_error):
        self._bridge = bridge
        self._is_error = is_error

    @property
    def encoding(self):
        return "utf-8"

    def writable(self):
        return True

    def isatty(self):
        return False

    def write(self, s):
        if not isinstance(s, str):
            s = str(s)
        if s:
            self._bridge.write(s, self._is_error)
        return len(s)

    def writelines(self, lines):
        for line in lines:
            self.write(line)

    def flush(self):
        pass


class _Reader(io.TextIOBase):
    """Sustituto de sys.stdin: cada línea se pide a la consola de Ghost."""

    def __init__(self, bridge):
        self._bridge = bridge

    @property
    def encoding(self):
        return "utf-8"

    def readable(self):
        return True

    def isatty(self):
        return False

    def readline(self, size=-1):
        line = self._bridge.readLine("")
        if line is None:
            return ""
        return line + "\n"

    def read(self, size=-1):
        chunks = []
        while True:
            line = self.readline()
            if not line:
                break
            chunks.append(line)
        return "".join(chunks)

    def __iter__(self):
        return self

    def __next__(self):
        line = self.readline()
        if not line:
            raise StopIteration
        return line


def _print_exception(script_path):
    exc_type, exc_value, tb = sys.exc_info()
    # Quita los marcos internos de runpy para que el traceback empiece en el código del usuario.
    internal = (__file__, os.path.abspath(__file__), "runpy.py", "<frozen runpy>")
    while tb is not None:
        filename = tb.tb_frame.f_code.co_filename
        if filename in internal or os.path.basename(filename) == "runpy.py":
            tb = tb.tb_next
        else:
            break
    traceback.print_exception(exc_type, exc_value, tb, file=sys.stderr)


def run_file(path, bridge):
    """Ejecuta `path` como __main__. Devuelve el código de salida (0 = correcto)."""
    _state["thread_id"] = threading.get_ident()
    workdir = os.path.dirname(path) or "."

    saved_stdout, saved_stderr, saved_stdin = sys.stdout, sys.stderr, sys.stdin
    saved_input = builtins.input
    saved_argv = list(sys.argv)
    saved_path = list(sys.path)
    saved_cwd = os.getcwd()
    modules_before = set(sys.modules)

    out = _Writer(bridge, False)
    err = _Writer(bridge, True)
    sys.stdout, sys.stderr, sys.stdin = out, err, _Reader(bridge)

    def ghost_input(prompt=""):
        if prompt:
            out.write(str(prompt))
        line = bridge.readLine(str(prompt))
        if line is None:
            raise EOFError("EOF when reading a line")
        return line

    builtins.input = ghost_input

    exit_code = 0
    try:
        os.chdir(workdir)
        sys.path.insert(0, workdir)
        sys.argv = [path]
        runpy.run_path(path, run_name="__main__")
    except SystemExit as exc:
        code = exc.code
        if code is None:
            exit_code = 0
        elif isinstance(code, int):
            exit_code = code
        else:
            err.write(str(code) + "\n")
            exit_code = 1
    except KeyboardInterrupt:
        exit_code = -2
    except BaseException:
        _print_exception(path)
        exit_code = 1
    finally:
        try:
            sys.stdout.flush()
        except Exception:
            pass
        sys.stdout, sys.stderr, sys.stdin = saved_stdout, saved_stderr, saved_stdin
        builtins.input = saved_input
        sys.argv = saved_argv
        sys.path[:] = saved_path
        try:
            os.chdir(saved_cwd)
        except OSError:
            pass
        # Olvida los módulos del propio proyecto para que la próxima ejecución
        # vuelva a importarlos con los cambios que se hayan guardado.
        for name in list(sys.modules):
            if name in modules_before:
                continue
            module_file = getattr(sys.modules.get(name), "__file__", None)
            if module_file and os.path.abspath(module_file).startswith(os.path.abspath(workdir)):
                sys.modules.pop(name, None)
        _state["thread_id"] = None
    return exit_code


def interrupt():
    """Lanza KeyboardInterrupt en el hilo que está ejecutando el programa del usuario."""
    thread_id = _state.get("thread_id")
    if thread_id is None:
        return False
    try:
        affected = ctypes.pythonapi.PyThreadState_SetAsyncExc(
            ctypes.c_ulong(thread_id), ctypes.py_object(KeyboardInterrupt)
        )
        return affected == 1
    except Exception:
        return False
