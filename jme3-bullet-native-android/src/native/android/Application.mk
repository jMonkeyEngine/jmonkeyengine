APP_OPTIM := release
APP_ABI := all
# Used to be stlport_static, but that has been removed.
APP_STL := c++_static
APP_MODULES      := bulletjme
APP_CFLAGS += -funroll-loops -Ofast

