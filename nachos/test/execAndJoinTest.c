#include "syscall.h"

int main() {
    int pid = exec("execAndJoinTest.coff", 0, 0);
    int status;
    join(pid, &status);
    printf("This line should be printed 4 times\n");
    exit(0);
}
