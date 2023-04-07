## 简介
> [中文](README.md) | [English](README_ENUS.md)

这是[阿里云应用诊断分析平台(ATP)](https://www.aliyun.com/product/developerservices/atp)的数据源生成工具，它可以帮助您生成Java堆转储(Heap dump)文件、Java线程栈日志等，它支持仅包含JRE的环境，以及AlpineJDK等特殊环境，可以运行在Windows/MacOS/Linux，目标虚拟机支持JDK6到JDK20。

# 作为命令行工具使用
您可以编译、构建出jar包，并通过Java命令行直接执行它
```sh
$ mvn package
$ java -Xbootclasspath/a:atp-client-api/src/main/resources/atp-client-tool.jar -jar atp-client-api/src/main/resources/atp-client-tool.jar
```
参数如下
```sh
Usage:
Main <pid> <subcommand> <options>

Subcommands:
heap                          Generate heap dump of Java process
                              [-file(mandatory), -object(mandatory)]
thread                        Print all threads and their stack traces
                              [-lock(optional), -extend(optional)]
list_heap                     List class and number of instance in Java heap
compiler_codeheap_analytics   Print CodeHeap analytics
compiler_codecache            Print code cache layout and bounds.
compiler_codelist             Print all compiled methods in code cache that are alive
compiler_directives_add       Add compiler directives from file.
compiler_directives_clear     Remove all compiler directives.
compiler_directives_print     Print all active compiler directives.
compiler_directives_remove    Remove latest added compiler directive.
compiler_queue                Print methods queued for compilation.
gc_class_histogram            Provide statistics about the Java heap usage.
gc_class_stats                Provide statistics about Java class meta data.
gc_finalizer_info             Provide information about Java finalization queue.
gc_heap_dump                  Generate a HPROF format dump of the Java heap.
gc_heap_info                  Provide generic Java heap information.
gc_run                        Call java.lang.System.gc().
gc_run_finalization           Call java.lang.System.runFinalization().
jfr_check                     Checks running JFR recording(s)
jfr_configure                 Configure JFR
jfr_dump                      Copies contents of a JFR recording to file. Either the name or the recording id must be specified.
jfr_start                     Starts a new JFR recording
jfr_stop                      Stops a JFR recording
jvmti_agent_load              Load JVMTI native agent.
jvmti_data_dump               Signal the JVM to do a data-dump request for JVMTI.
managementagent_start         Start remote management agent.
managementagent_start_local   Start local management agent.
managementagent_status        Print the management agent status.
managementagent_stop          Stop remote management agent.
thread_print                  Print all threads with stacktraces.
vm_check_commercial_features  Obsolete
vm_class_hierarchy            Print a list of all loaded classes, indented to show the class hiearchy. The name of each class is followed by the ClassLoaderData* of its ClassLoader, or "null" if loaded by the bootstrap class loader.
vm_classloader_stats          Print statistics about all ClassLoaders.
vm_classloaders               Prints classloader hierarchy.
vm_command_line               Print the command line used to start this VM instance.
vm_dynlibs                    Print loaded dynamic libraries.
vm_flags                      Print VM flag options and their current values.
vm_info                       Print information about JVM environment and status.
vm_log                        Lists current log configuration, enables/disables/configures a log output, or rotates all logs.
vm_metaspace                  Prints the statistics for the metaspace
vm_native_memory              Print native memory usage
vm_print_touched_methods      Print all methods that have ever been touched during the lifetime of this JVM.
vm_set_flag                   Sets VM flag option using the provided value.
vm_stringtable                Dump string table.
vm_symboltable                Dump symbol table.
vm_system_properties          Print system properties.
vm_systemdictionary           Prints the statistics for dictionary hashtable sizes and bucket length
vm_unlock_commercial_features Obsolete
vm_uptime                     Print VM uptime.
vm_version                    Print JVM version information.

```

# 作为Java SDK使用
您可以添加如下Maven依赖并通过SDK方式调用：
```
<dependency>
  <groupId>com.aliyun.atp</groupId>
  <artifactId>atp-client-api</artifactId>
  <version>1.0.2</version>
</dependency>
```
```java
import com.aliyun.atp.tool.ATPClient;
import com.aliyun.atp.tool.ClientException;

public class SDKExample {
    public static void main(String[] args) {
        try {
            ATPClient.execute(null);
            ATPClient.execute(new String[]{"<pid>", "thread"});
            ATPClient.execute(new String[]{"<pid>", "full_jcmd"});
            ATPClient.execute(new String[]{"<pid>", "heap", "-file=/tmp/heapdump.hprof","-object=all"});
            ATPClient.execute(new String[]{"<pid>", "heap", "-file=/tmp/heapdump.hprof","-object=live"});
        } catch (ClientException e) {
            e.printStackTrace();
        }
    }
}
```
