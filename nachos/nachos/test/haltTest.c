#include "syscall.h"

int main() {
    int pid = exec("haltTest.coff", 0, 0);
    int status;
    join(pid, &status);
    halt();
    printf("You should see this line 3 times\n");
}
