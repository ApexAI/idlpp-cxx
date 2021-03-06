/*
 *                         OpenSplice DDS
 *
 *   This software and documentation are Copyright 2006 to 2015 PrismTech
 *   Limited and its licensees. All rights reserved. See file:
 *
 *                     $OSPL_HOME/LICENSE
 *
 *   for full copyright notice and license terms.
 *
 */

/** \file os/darwin/code/os_time.c
 *  \brief Darwin time management
 *
 * Implements time management for Darwin
 */
#include <os_errno.h>
#include <stdint.h>
#include <sys/time.h>
#include <mach/mach_time.h>

#include <../common/code/os_time.c>
#include <../common/code/os_time_ctime.c>


/*  \brief Get the current time
 */
static os_time
os__timeDefaultTimeGet(void)
{
    static int timeshift = INT_MAX;
    struct timeval tv;
    os_time rt;

    if(timeshift == INT_MAX) {
        const char *p = getenv("OSPL_TIMESHIFT");
        timeshift = (p == NULL) ? 0 : atoi(p);
    }

    (void) gettimeofday (&tv, NULL);

    rt.tv_sec = (os_timeSec) tv.tv_sec + timeshift;
    rt.tv_nsec = tv.tv_usec*1000;

    return rt;
}

/** \brief Get high resolution relative time
 *
 */
os_time
os_timeGetMonotonic (
    void
    )
{
    static mach_timebase_info_data_t timeInfo;
    os_time t;
    uint64_t mt;
    uint64_t mtNano;

    /* The Mach absolute time returned by mach_absolute_time is very similar to
     * the QueryPerformanceCounter on Windows. The update-rate isn't fixed, so
     * that information needs to be resolved to provide a clock with real-time
     * progression.
     *
     * The mach_absolute_time does include time spent during sleep (on Intel
     * CPU's, not on PPC), but not the time spent during suspend.
     *
     * The result is not adjusted based on NTP, so long-term progression by this
     * clock may not match the time progression made by the real-time clock. */
    mt = mach_absolute_time();

    if( timeInfo.denom == 0) {
        (void) mach_timebase_info(&timeInfo);
    }
    mtNano = mt * timeInfo.numer / timeInfo.denom;
    t.tv_sec = (os_timeSec) (mtNano / 1000000000);
    t.tv_nsec = (os_int32) (mtNano % 1000000000);

    return t;
}

/** \brief Get elapsed time.
 *
 * \return elapsed time since some unspecified fixed past time
 * \return os_timeGetMonotonic() otherwise
 */
os_time
os_timeGetElapsed (
    void
    )
{
    /* Elapsed time clock not (yet) supported on this platform. */
    return os_timeGetMonotonic();
}

/** \brief Suspend the execution of the calling thread for the specified time
 *
 * \b os_nanoSleep suspends the calling thread for the required
 * time by calling \b nanosleep. First it converts the \b delay in
 * \b os_time definition into a time in \b struct \b timeval definition.
 * In case the \b nanosleep is interrupted, the call is re-entered with
 * the remaining time.
 */
os_result
os_nanoSleep (
    os_time delay)
{
    struct timespec t;
    struct timespec r;
    int result;
    os_result rv;

    if( delay.tv_sec >= 0 && delay.tv_nsec >= 0) {
        /* Time should be normalized */
        assert (delay.tv_nsec < 1000000000);
        t.tv_sec = delay.tv_sec;
        t.tv_nsec = delay.tv_nsec;
        result = nanosleep (&t, &r);
        while (result && os_getErrno() == EINTR) {
            t = r;
            result = nanosleep (&t, &r);
        }
        if (result == 0) {
            rv = os_resultSuccess;
        } else {
            rv = os_resultFail;
        }
    } else {
        rv = os_resultFail;
    }
    return rv;
}
