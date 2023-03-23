#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include "err.h"

#define NR_PROC 5

void new_fork(int cnt) {
    if (!cnt) return;
    switch (fork()) {
        case -1:
            syserr("Error in fork\n");

        case 0:
            printf("My parent is %d and I am %d\n", getppid(), getpid());
            new_fork(cnt - 1);
            exit(0);

        default:
            if (wait(0) == -1)
                syserr("Error in wait\n");
            printf("parent of pid %d finished waiting for children\n", getpid());
    }
}

int main ()
{
    new_fork(NR_PROC);
    return 0;
}


