#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
int main(int argc, char** argv)
{
 int i = 0;
 while(i++ < 5) {
 	printf("i=%d\n", i);
 }
 exit(0);
}
