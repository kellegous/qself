#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <termios.h>

namespace {

const char* kTtyDevice = "/dev/tty.usbmodem80131";

} // anonymous

int main(int argc, char* argv[]) {
    int usb = open(kTtyDevice, O_RDWR| O_NDELAY);
    if (!usb) {
        fprintf(stderr, "open of %s failed.", kTtyDevice);
        return 1;
    }

    struct termios tty;
    memset(&tty, 0, sizeof(tty));

    if (tcgetattr(usb, &tty) != 0) {
        fprintf(stderr, "tcgetattr failed\n");
        return 1;
    }

    /* Set Baud Rate */
    cfsetospeed(&tty, B9600);
    cfsetispeed(&tty, B9600);

    tty.c_cflag = B9600 | CS8 | CLOCAL | CREAD;
    tty.c_iflag = IGNPAR;
    tty.c_oflag = 0;
    tty.c_lflag = 0;
    tty.c_cc[VTIME] = 1;  // timeout after .1s that isn't working
    tty.c_cc[VMIN] = 64;  // want to read a chunk of 64 bytes at a given time

    // tty.c_cflag &= ~PARENB;        // Make 8n1
    // tty.c_cflag &= ~CSTOPB;
    // tty.c_cflag &= ~CSIZE;
    // tty.c_cflag |= CS8;
    // tty.c_cflag &= ~CRTSCTS;       // no flow control
    // tty.c_lflag = 0;          // no signaling chars, no echo, no canonical processing
    // tty.c_oflag = 0;                  // no remapping, no delays
    // tty.c_cc[VMIN] = 1;                  // read doesn't block
    // tty.c_cc[VTIME] = 5;                  // 0.5 seconds read timeout

    // tty.c_cflag |= CREAD | CLOCAL;     // turn on READ & ignore ctrl lines
    // tty.c_iflag &= ~(IXON | IXOFF | IXANY);// turn off s/w flow ctrl
    // tty.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG); // make raw
    // tty.c_oflag &= ~OPOST;              // make raw

    tcflush(usb, TCIFLUSH);

    if (tcsetattr (usb, TCSANOW, &tty) != 0) {
        fprintf(stderr, "tcsetattr failed.\n");
        return 1;
    }

    char buf[256];
    memset(&buf, '\0', sizeof(buf));

    fd_set fds;

    FD_SET(usb, &fds);

    while (true) {
        select(usb + 1, &fds, NULL, NULL, NULL);
        if (!FD_ISSET(usb, &fds)) {
            continue;
        }

        int n = read(usb, &buf, sizeof(buf) - 1);
        if (n < 0) {
            continue;
        }
        buf[n] = '\0';
        printf("%s", buf);
    }

    return 0;
}