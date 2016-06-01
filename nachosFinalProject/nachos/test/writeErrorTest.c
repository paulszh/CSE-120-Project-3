#include "syscall.h"
int main() {
   int writtenBytes;
   char buffer[1024] = "Should not be in the file\n";
   int fd = open("GuyFawkes.txt");
   int badFd = open("GuyFAWKES.txt");
   int protFd = open("R0W0.txt");

   writtenBytes = write(-1, buffer, 1024);
   if (writtenBytes == -1) {
      printf("Bad fd: Fatal Error Encoutered; Expected;\n");
   } else {
      printf("Bad fd: Fatal Error Not Encountered; Unexpected;\n");
   }

   writtenBytes = write(1024, buffer, 1024);
   if (writtenBytes == -1) {
      printf("Bad fd: Fatal Error Encoutered; Expected;\n");
   } else {
      printf("Bad fd: Fatal Error Not Encountered; Unexpected;\n");
   }


   writtenBytes = write(badFd, buffer, 1024);
   if (writtenBytes == -1) {
      printf("Bad fd: Fatal Error Encoutered; Expected;\n");
   } else {
      printf("Bad fd: Fatal Error Not Encountered; Unexpected;\n");
   }

   writtenBytes = write(fd, buffer, -1);
   if (writtenBytes == -1) {
      printf("Bad num of byte: Fatal Error Encoutered; Expected;\n");
   } else {
      printf("Bad num of byte: Fatal Error Not Encountered; Unexpected;\n");
   }

   writtenBytes = write(fd, -1, 1024); 
   if (writtenBytes == -1) {
      printf("Bad Buffer: Fatal Error Encoutered; Expected;\n");
   } else {
      printf("Bad Buffer: Fatal Error Not Encountered; Unexpected;\n");
   }

   writtenBytes = write(protFd, buffer, 1024); 
   if (writtenBytes == -1) {
      printf("Can't Write File: Fatal Error Encoutered; Expected;\n");
   } else {
      printf("Can't Write File: Fatal Error Not Encountered; Unexpected;\n");
   }

   halt();
   return 0;
}
