
//In order to better my own understanding, I tried to create a proper deadlock,
//Which is caused when a thread tries to access a resource being used by something else.


public class OtherApp {

    public static void main(String[] args){

        final String resource = "hello";
        final String otherResource = "world";

        Thread thread1 = new Thread(){
            public void run() {

                synchronized (resource) {
                    System.out.println("Thread 1 has first resource locked: " + resource);

                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {

                    }

                    synchronized (otherResource) {
                        System.out.println("Thread 2 has second resource locked: " + otherResource);
                    }

                }
            }
        };

        Thread thread2 = new Thread() {
            public void run() {

                synchronized (otherResource) {
                    System.out.println("Thread 2 has first resource locked: " + otherResource);

                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {

                    }

                    synchronized (resource) {
                        System.out.println("Thread 2 has second resource locked: " + resource);
                    }

                }
            }
        };

        thread1.start();
        thread2.start();


    }



}
