#include "syscall.h"

int main() {
    int pid = exec("execTest.coff", 0, 0);
    printf("will keep printing, kill it by ctrl-c\n");
    exit(0);
}
