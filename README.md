# diyTomcat
这个项目是作者参考Tomcat源码，自制的一个具备servlet容器功能的web服务器。用户需要配置server.xml对web应用进行部署，配置信息包括端口号、web应用文件存放的绝对路径、web应用匹配的请求路径等。
部署完毕后再启动BootStrap类运行diyTomcat，此时便可接收浏览器请求进行处理。DiyTomcat参考Tomcat的系统架构，实现了Server、service、Connector、Engine、Host、Context，Server主要负责日志
打印，我们日常启动应用时打印出来的日志文件大多都在Server层面打印，Service主要负责新建Connector和Engine，通过线程池去创建多个Connector去监听多个端口，Connector通过socket监听某个端口，
根据浏览器访问请求解析出对应的信息封装在request对象当中，例如：cookie，访问路径，访问参数，是否压缩等，Httpprocess类则去根据request对象去实现相应的逻辑，例如根据request的访问路径得到
对应的Context，Context就是我们的web应用，每个web应用中又有对应的web.xml文件去配置对应是servlet（也就是我们所说的控制类），再通过反射去实现对应的servlet类执行相应的方法（doGet doPost）,得到的结果封装成response对象传出，最后再根据http协议拼接发送给浏览器。Engine主要负责生成Host对象，他们是一对多的关系，生成的Host对象维护在一个hashmap中，Host负责去扫描server.xml文件，因为部署在diyTomcat的web应用不止一个，支持多个应用同时部署在diyTomcat中，Host负责扫描server.xml中的多应用去存放在hashmap中，hashmap以请求路径和Context想映射，request能够通过请求路径找到Context也正在传入Host对象实现的，Context则是由web应用抽象出来的类，由Context存放web应用对应的信息，例如访问路径、存放路径、自定义类加载器、servlet匹配路径等等。

整个项目大概的流程就是浏览器向diyTomcat发送访问请求，由Connector接收以后将请求信息封装在request对象当中，再通过HttpProcess类用反射得到request对象当中的servlet类以及调用对应的方法，得到的结果封装成response对象根据http协议发送给浏览器。这个同时也是最简单的应用场景，而实际开发我们还需要做一些工作以满足不同的应用场景，例如session管理，我们需要维护一个hashmap将浏览器的cookieId与session对象映射起来，以便在需要时可以拿取session中的信息，同时创建一个监听线程去监听该对象是否过期，如果过期则需要将对应的session从hashmap清除；客户端跳转以及服务端跳转，在diyTomcat，客户端跳转是由http协议完成的，在服务器发送浏览器信息时code为302，浏览器接收后就会按照指定路径再次访问以实现客户端跳转，可服务端跳转则是修改request中的访问路径再重新运行一次从而实现服务端跳转；多端口，实际运用当中，我们需要监听多个端口去访问web应用，例如我们可以同时监听8080和18080，在diyTomcat中就用到了线程池去创建多个Connector去监听多个端口；热部署，diyTomcat创建监听线程去监听web应用的存放路径，如果发生改动则重新进行加载以实现热部署；责任链，在日常应用中我们可能需要定义一些过滤器以处理我们的请求，将请求细化，使得责任链中的每一个链条都去处理相关的逻辑，从而有利于我们的代码扩展和错误定位，责任链需要在对应的web应用配置文件中去定义，原理跟servlet类似，只不过责任链得到的filter类是一个集合，我们需要将其遍历执行，最终再执行serlvet；自定义类加载器，参考Tomcat打破双亲委派机制，我们在Context中都需要新建一个web应用类加载器，同时也需要新建JSP类加载器去继承应用类加载器，重写loadclass方法去改写类加载顺序，同时JSP类加载在实际的应用当中是经常被Web应用抛弃以实现JSP热加载，那么我们需要维护一个hashmap将JSP文件以及对应的JSP类加载器对应起来，新建监听线程去监听改动，当出现改动时将hashmap中的JSP类加载器清除再新建新的JSP类加载器等等。通过这些补充，diyTomcat的应用场景也将趋于完善，但仍然不足的是diyTomcat只支持mvc架构的web应用部署，不支持注解，同时也没办法做到集群，这也是diyTomcat与Tomcat之间的差距，Tomcat的应用场景已经是非常成熟且完善了，diyTomcat还有很长的路要走。
