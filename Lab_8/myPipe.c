#include <unistd.h>
#include <string.h>
#include "err.h"

int main(int argc, char *argv[]) {

    int pipe_dsc[2];

    for (int i = 1; i < argc - 1; i++) {
        char *command = argv[i];

        if (pipe(pipe_dsc) == -1) {
            syserr("Error in pipe\n");
        }

        switch (fork()) {
            case -1:
                syserr("Error in fork\n");
                break;

            case 0: // we are parent
                dup2(pipe_dsc[1], STDOUT_FILENO);
                close(pipe_dsc[0]);
                close(pipe_dsc[1]);
                execlp(command, command, NULL);
                return -1;

            default: // we are child
                dup2(pipe_dsc[0], STDIN_FILENO);
                close(pipe_dsc[0]);
                close(pipe_dsc[1]);
        }
    }

    // final command execution
    execlp(argv[argc - 1], argv[argc - 1], NULL);
    return -1;
}