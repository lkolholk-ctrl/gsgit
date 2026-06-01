# O-MVLL Setup

Скачай `libOMVLL.so` для Android NDK из релизов:
https://github.com/open-obfuscator/o-mvll/releases

Выбери версию под твой NDK (r27 / r26 / r25).
Положи файл сюда: `app/omvll/libOMVLL.so`

Без этого файла проект собирается без обфускации (warning в cmake).

## Применяемые passes:
- **StringEncryption** — все строки зашифрованы (пути, маркеры Frida)
- **ControlFlowFlattening** — все security функции
- **OpaqueConstants** — числовые константы (порты и т.д.)
- **BogusControlFlow** — detection функции
- **ArithmeticSubstitution** — весь модуль
