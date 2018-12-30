APP_OPTIM := release
APP_ABI := all
APP_STL := stlport_static
# gnustl_static or stlport_static
APP_MODULES      := bulletjme
APP_CFLAGS += -funroll-loops -Ofast

