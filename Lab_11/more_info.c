#include <errno.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

#include "err.h"

#define N_CHILDREN 5
#define MY_SIGNAL SIGUSR1
// #define MY_SIGNAL (SIGRTMIN + 1)

void handler(int sig, siginfo_t* info, void* more)
{
    // Printf is not really safe in a signal handler, but we leave it here for simplicity.
    printf("Parent: got signal >>%s<< from %d\n", strsignal(sig), info->si_pid);
}

int child(pid_t parent_pid)
{
    printf("Child %d: sending signal\n", getpid());
    CHECK_ERRNO(kill(parent_pid, MY_SIGNAL));
    printf("Child %d: ending.\n\n", getpid());
    return 0;
}

int main()
{
    pid_t parent_pid = getpid();

    // Install signal handler for MY_SIGNAL.
    struct sigaction action;
    sigemptyset(&action.sa_mask);
    action.sa_sigaction = handler;
    action.sa_flags = SA_SIGINFO;
    CHECK_ERRNO(sigaction(MY_SIGNAL, &action, NULL));

    // Start children.
    for (int i = 0; i < N_CHILDREN; ++i) {
        pid_t child_pid;
        CHECK_ERRNO(child_pid = fork());
        if (!child_pid)
            return child(parent_pid);
        else
            continue;
    }

    // Block MY_SIGNAL. Some signals may get delivered before that.
    sigset_t block_mask;
    sigemptyset(&block_mask);
    sigaddset(&block_mask, MY_SIGNAL);
    CHECK_ERRNO(sigprocmask(SIG_BLOCK, &block_mask, NULL));

    printf("Parent: sleeping.\n");
    sleep(5);

    // Unblock MY_SIGNAL. Remaining signals only raise the handler at most once.
    CHECK_ERRNO(sigprocmask(SIG_UNBLOCK, &block_mask, NULL));

    // Wait for children.
    for (int i = 0; i < N_CHILDREN; ++i) {
        wait(0);
    }

    printf("Parent: ending.\n");
    return 0;
}
