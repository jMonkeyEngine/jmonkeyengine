/* API declaration export attribute */
#define AL_API  __attribute__((visibility("protected")))
#define ALC_API __attribute__((visibility("protected")))

/* Define to the library version */
#define ALSOFT_VERSION "1.16.0"

#ifdef IN_IDE_PARSER
/* KDevelop's parser doesn't recognize the C99-standard restrict keyword, but
 * recent versions (at least 4.5.1) do recognize GCC's __restrict. */
#define restrict __restrict
#endif 

/* Define any available alignment declaration */
#define ALIGN(x) __attribute__((aligned(x)))

/* Define if we have the C11 aligned_alloc function */
/* #undef HAVE_ALIGNED_ALLOC */

/* Define if we have the posix_memalign function */
/* #undef HAVE_POSIX_MEMALIGN */

/* Define if we have the _aligned_malloc function */
/* #undef HAVE__ALIGNED_MALLOC */

/* Define if we have SSE CPU extensions */
/* #undef HAVE_SSE */
/* #undef HAVE_SSE2 */
/* #undef HAVE_SSE4_1 */

/* Define if we have ARM Neon CPU extensions */
/* #undef HAVE_NEON */

/* Define if we have FluidSynth support */
/* #undef HAVE_FLUIDSYNTH */

/* Define if we have the ALSA backend */
/* #undef HAVE_ALSA */

/* Define if we have the OSS backend */
/* #undef HAVE_OSS */

/* Define if we have the Solaris backend */
/* #undef HAVE_SOLARIS */

/* Define if we have the SndIO backend */
/* #undef HAVE_SNDIO */

/* Define if we have the QSA backend */
/* #undef HAVE_QSA */

/* Define if we have the MMDevApi backend */
/* #undef HAVE_MMDEVAPI */

/* Define if we have the DSound backend */
/* #undef HAVE_DSOUND */

/* Define if we have the Windows Multimedia backend */
/* #undef HAVE_WINMM */

/* Define if we have the PortAudio backend */
/* #undef HAVE_PORTAUDIO */

/* Define if we have the PulseAudio backend */
/* #undef HAVE_PULSEAUDIO */

/* Define if we have the CoreAudio backend */
/* #undef HAVE_COREAUDIO */

/* Define if we have the OpenSL backend */
#define HAVE_OPENSL

/* Define if we have the Wave Writer backend */
#define HAVE_WAVE

/* Define if we have the stat function */
#define HAVE_STAT

/* Define if we have the lrintf function */
#define HAVE_LRINTF

/* Define if we have the strtof function */
/* #undef HAVE_STRTOF */

/* Define if we have the __int64 type */
/* #undef HAVE___INT64 */

/* Define to the size of a long int type */
#define SIZEOF_LONG 4

/* Define to the size of a long long int type */
#define SIZEOF_LONG_LONG 8

/* Define if we have C99 variable-length array support */
#define HAVE_C99_VLA

/* Define if we have C99 _Bool support */
#define HAVE_C99_BOOL

/* Define if we have C11 _Static_assert support */
#define HAVE_C11_STATIC_ASSERT

/* Define if we have C11 _Alignas support */
/* #undef HAVE_C11_ALIGNAS */

/* Define if we have C11 _Atomic support */
/* #undef HAVE_C11_ATOMIC */

/* Define if we have GCC's destructor attribute */
#define HAVE_GCC_DESTRUCTOR

/* Define if we have GCC's format attribute */
#define HAVE_GCC_FORMAT

/* Define if we have stdint.h */
#define HAVE_STDINT_H

/* Define if we have stdbool.h */
#define HAVE_STDBOOL_H

/* Define if we have stdalign.h */
/* #undef HAVE_STDALIGN_H */

/* Define if we have windows.h */
/* #undef HAVE_WINDOWS_H */

/* Define if we have dlfcn.h */
#define HAVE_DLFCN_H

/* Define if we have pthread_np.h */
/* #undef HAVE_PTHREAD_NP_H */

/* Define if we have alloca.h */
/* #undef HAVE_ALLOCA_H */

/* Define if we have malloc.h */
#define HAVE_MALLOC_H

/* Define if we have ftw.h */
/* #undef HAVE_FTW_H */

/* Define if we have io.h */
/* #undef HAVE_IO_H */

/* Define if we have strings.h */
#define HAVE_STRINGS_H

/* Define if we have cpuid.h */
/* #undef HAVE_CPUID_H */

/* Define if we have intrin.h */
/* #undef HAVE_INTRIN_H */

/* Define if we have sys/sysconf.h */
#define HAVE_SYS_SYSCONF_H

/* Define if we have guiddef.h */
/* #undef HAVE_GUIDDEF_H */

/* Define if we have initguid.h */
/* #undef HAVE_INITGUID_H */

/* Define if we have ieeefp.h */
/* #undef HAVE_IEEEFP_H */

/* Define if we have float.h */
#define HAVE_FLOAT_H

/* Define if we have fenv.h */
#define HAVE_FENV_H

/* Define if we have GCC's __get_cpuid() */
/* #undef HAVE_GCC_GET_CPUID */

/* Define if we have the __cpuid() intrinsic */
/* #undef HAVE_CPUID_INTRINSIC */

/* Define if we have _controlfp() */
/* #undef HAVE__CONTROLFP */

/* Define if we have __control87_2() */
/* #undef HAVE___CONTROL87_2 */

/* Define if we have ftw() */
/* #undef HAVE_FTW */

/* Define if we have _wfindfirst() */
/* #undef HAVE__WFINDFIRST */

/* Define if we have pthread_setschedparam() */
#define HAVE_PTHREAD_SETSCHEDPARAM

/* Define if we have pthread_setname_np() */
#define HAVE_PTHREAD_SETNAME_NP

/* Define if we have pthread_set_name_np() */
/* #undef HAVE_PTHREAD_SET_NAME_NP */

/* Define if we have pthread_mutexattr_setkind_np() */
/* #undef HAVE_PTHREAD_MUTEXATTR_SETKIND_NP */

/* Define if we have pthread_mutex_timedlock() */
/* #undef HAVE_PTHREAD_MUTEX_TIMEDLOCK */