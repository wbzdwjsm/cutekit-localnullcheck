# cutekit-localnullcheck
Most annotations with names like "NotNull""NonNull""NoNull" and so on are used for checking whether fields/parameters are null at runtime, but this cutekit is used for cheching whether local variables are null, and when local variables get null at runtime, you can even replace null with your own method besides throwing NullPointerException. This kit is based on APT.

Put the @NullCheck annotation on your method, meaning "Checking all local variables in this method". 
```java
    @NullCheck
    public static void methodA() {
        List s = null;
        System.out.println(s);
        s = new ArrayList();
        System.out.println(s);
        s = invoke();
        System.out.println(s);
    }
```
When compiled, the .class file looks like:
```java
    public static void methodA() {
        List s = null;  //(1)
        System.out.println(s); 
        s = new ArrayList();  //(2)
        System.out.println(s);
        s = invoke(); //(3)
        if (s == null) {
            throw new NullPointerException("List:s cannot be null!");
        } else {
            System.out.println(s);
        }
    }
```
## Introduction: 
### (1) List s = null; this will not be processed because this assignment is your explicit decision!
### (2) s = new ArrayList(); this will not be processed because "new" operation never leads to null!
### (3) s = invoke(); this will be processed, of course. Invocation of a method may make a null object.

If you don't want to check any local variables, just put @Exclude annotation on them. 
