#include <errno.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

#include "err.h"

int main(void)
{
    // Block SIGINT (add it to the process' signal mask).
    sigset_t block_mask;
    sigemptyset(&block_mask);
    sigaddset(&block_mask, SIGINT);
    CHECK_ERRNO(sigprocmask(SIG_BLOCK, &block_mask, NULL));

    printf("My pid is %d\n", getpid());
    printf("I will block SIGINT for 5 seconds.\n");
    printf("Try signalling me with Ctrl + C or by executing:\n");
    printf("kill -SIGINT %d\n", getpid());

    sleep(5);

    sigset_t pending_mask;
    sigpending(&pending_mask);
    if (sigismember(&pending_mask, SIGINT)) {
        printf("Pending signal: %s\n", strsignal(SIGINT));
        printf("I'll unblock it now an the defaul handler will terminate.\n");
    }

    CHECK_ERRNO(sigprocmask(SIG_UNBLOCK, &block_mask, NULL));

    printf("I got no signal.\n");
    return 0;
}
