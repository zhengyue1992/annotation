版本：1.0.2

### 一、说明：

​        当你代码中含有大量的if else if ..注解，在if else中又有需要处理的逻辑，这时你的代码就会变的非常繁琐，维护起来非常不方便，而策略模式就是来帮你解决这个问题的。传统的策略模需要创建接口，通过策略创建不同的实现类来解决这个问题的，但这样一来，就会出现大量的类，数量一多，维护起来也会很麻烦。

​	这些代码是，基于方法的策略模式，依赖于spring容器，避免创建大量的类，只需要在你需要的方法上添加@PolicyType注解，将对应的策略注入即可。相同的策略会覆盖

### 二、使用方法

​	1、在你自己的方法上添加@PolicyType注解，并将策略传入

​	2、让该类实现PolicyInterface接口（该接口只用于识别，没有具体需要实现的方法）

​	3、在该类上添加@Component注解，将该类交由spring容器管理

​	4、在需要执行策略的类中，使用@Autowired注入PolicyActuator类的实例。

​	5、调用PolicyActuator的runPolicy 方法，程序会根据你传入的策略，自动匹配对应的方法。

​	附:关于spel表达式的使用

```java
* 使用@占位要传入的值(@也是必须传入的，否则会报错)
* 判断大于6： @>6 或者 @>2*3 等
* 判断小于6： @<6 或者 @<2*3 等
* 判断某个数介于1-100之间： @ between {1,100}
* 逻辑运算符：@>6 or @<0; @>6 and @<100
```

### 三、改进

​	1、该版本添加了spel表达式，可以使用spel表达式来匹配你的规则。

​	2、添加group的概念，可以对策略进行分组，然后根据分组进行筛选。