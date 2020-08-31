# cutekit-localnullcheck
Most annotations with names like "NotNull""NonNull""NoNull" and so on are used for checking whether fields/parameters are null at runtime, but this cutekit is used for cheching whether local variables are null, and when local variables get null at runtime, you can even replace null with your own method besides throwing NullPointerException. This kit is based on APT.

Put the @NullCheck annotation on your method, meaning "Checking all local variables in this method". 
java code:
```java
    @NullCheck
    public static void methodA() {
        List s = null;
        System.out.println(s);
        s = new ArrayList();
        System.out.println(s);
        s = invoke(); //Invoking a method to return a List object.
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
### (1) List s = null; this will not be processed because this assignment is your explicit decision.
### (2) s = new ArrayList(); this will not be processed because "new" operation never leads to null.
### (3) s = invoke(); this will be processed, of course. Invocation of a method may make a null object.

If you don't want to check any local variables, just put @Exclude annotation on them. 
java code:
```java
    @NullCheck
    public static void methodA() {
        @Exclude
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
        List s = null;  
        System.out.println(s); 
        s = new ArrayList(); 
        System.out.println(s);
        s = invoke(); 
        System.out.println(s);
    }
```
Yes, just the same like your .java file.

At the point where nulls may occur, if you don't want to throw NullPointerException, you can also use @Indicator.
java code:
```java
    public static void methodA() {
        @Indicator(procType = ProcType.NEW, newClass = CopyOnWriteArrayList.class)  (1)
        List s = null;  
        System.out.println(s); 
        s = new ArrayList();  
        System.out.println(s);
        s = invoke(); 
        System.out.println(s);
    }
```
### (1): It means "When invoking a mothod to assign the returned value to s and null occurs, reassign local variable s using "new CopyOnWriteArrayList()".
When compiled, the .class file looks like:
```java
    public static void methodA() {
        List s = null;
        System.out.println(s);
        s = new ArrayList();
        System.out.println(s);
        List s = invoke();
        if (s == null) {
            s = new CopyOnWriteArrayList();
        }
        System.out.println(s);
    }
```
------------------------------
Is this kit nice? I don't know, it is only a demo. It cannot process those local varibles in if/for/switch/lambda/... blocks because I didn't process these blocks. In APT, "if" makes a JCIf, and "for" makes a JCForLoop, and "switch" makes a JCSwitch... Do I have to process these kinds of JCStatement one by one? Is there a general way to process them? If you konw, please teach me, thanks a lot!
