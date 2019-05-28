import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LRUcacheTest {
    public static void main(String[] args){
//        testDelete();//模拟秒杀
//        testGetPerformance();//get性能测试
        testPutPerformance();//put性能测试
//        testGetPut();//LRU缓存联合测试
//        testFistTail();//链表并发插入删除测试
//        testRemoveTail();//链表并发删除测试

//        testPutUniq();
//        testPutOver();
//        testGet();

    }
    static void testDelete(){
        //模拟秒杀：1亿人抢10个商品
        System.out.println("模拟秒杀：1亿人抢10个商品");
        LRUcache<Integer,Integer> ucache = new LRUcache<>(10);
        for(int i=0;i<10;i++)
            ucache.put(i,i);
        Long star = System.nanoTime();

        for(int i=0;i<100000000;i++){
            if(ucache.size()==0)
                continue;
            int kk=i;
            //随机抢第k个
            new Thread(()->{
                int k = ThreadLocalRandom.current().nextInt(10);
                if(ucache.delete(k)!=null)
                    System.out.println("id "+k+ " is owner by "+kk+" people");
            }).start();
        }

        while (Thread.activeCount()>2)
            Thread.yield();
        System.out.println("\nslip time "+(System.nanoTime()-star)/1000000+"ms");
        System.out.println(ucache);

    }
    static void testGetPerformance() {
        System.out.println("get性能测试：在双核四线程cpu上并发get性能是单线程的3+倍【key取之1-100，500万get】");
        LRUcache<Integer, Integer> link = new LRUcache<>(100);
        for(int i=0;i<100;i++)
            link.put(i,i);
        int n=20000;//线程数
        int kk=5000000/n;
        //测试建立5000线程时间
        long t1 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            new Thread(() -> {
            }).start();
        }
        long t2 = System.nanoTime();
        //用5000个线程：测试并发get500万次
        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                for (int j = 0; j < kk; j++) {
                    int k = ThreadLocalRandom.current().nextInt(100) ;
                    link.get(k);
                }
            }).start();
        }
        while (Thread.activeCount()>2)
            Thread.yield();

        long t3 = System.nanoTime();
        //用单线程：测试并发get500万次
        for(int i=0;i<5000000;i++) {
            int k = ThreadLocalRandom.current().nextInt(100) ;
            link.get(k);
        }
        long t4 = System.nanoTime();

        System.out.println("set up "+n+" Threads time "+(t2-t1)/1000000+"ms");
        System.out.println("slip time: "+((t3-t2-t2+t1)/1000000+"ms"+" for concurrent"));
        System.out.println("slip time: "+(t4-t3)/1000000+"ms"+" for single");
        System.out.println(link);


    }
    static void testPutPerformance() {
        System.out.println("put性能测试：在双核四线程cpu上并发put性能是单线程的2[非重复key]-3倍[重复key，相当于get]");
        System.out.println("put性能：没有get的3倍说明map的putIfAbsent分段锁性能比原生cas弱");
        LRUcache<Integer, Integer> link = new LRUcache<>(100);
        //测试建立5000线程时间
        int n=20000;
        int kk=5000000/n;
        long t1 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            new Thread(() -> {
            }).start();
        }
        long t2 = System.nanoTime();
        //用5000个线程：测试并发put500万次
        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                for (int j = 0; j < kk; j++) {
                    int k = ThreadLocalRandom.current().nextInt() ;
                    link.put(k,k);
                }
            }).start();
        }
        while (Thread.activeCount()>2)
            Thread.yield();

        long t3 = System.nanoTime();
        //用单线程：测试并发put500万次
        link.clear();
        for(int i=0;i<5000000;i++) {
            int k = ThreadLocalRandom.current().nextInt() ;
            link.put(k,k);
        }
        long t4 = System.nanoTime();

        System.out.println("set up "+n+" Threads time "+(t2-t1)/1000000+"ms");
        System.out.println("slip time: "+((t3-t2-t2+t1)/1000000+"ms"+" for concurrent"));
        System.out.println("slip time: "+(t4-t3)/1000000+"ms"+" for single");
        System.out.println(link);
    }
    static void testGetPut(){
        //用3000个线程：每个线程put/get 1000次,来检测最后100次请求顺序
        System.out.println("LRU缓存联合测试:用3000个线程：每个线程put/get各1000次,来检测最后100次请求顺序");
        System.out.println("联合测试：所展现的性能并没有单独测试性能好，是因为该测试用了原子计数器拖慢了3000线程之间整体速度");
        LRUcache<Integer, Integer> link = new LRUcache<>(100);


        int nThread=1000,nRequest=1000,n=3;
        int []data=new int[nThread*nRequest*n];
        AtomicInteger cnt = new AtomicInteger();
        long star = System.nanoTime();

        for (int i = 0; i < nThread; i++) {
            int kk=i*1000;
            new Thread(() -> {
                for (int j = 0; j < nRequest; j++) {
                    int k = kk+j;
                    if(link.get(k)!=null)
                        link.put(k,k);
                    data[cnt.incrementAndGet()-1]=k;
                }
            }).start();
            new Thread(() -> {
                for (int j = 0; j < nRequest; j++) {
                    int k = ThreadLocalRandom.current().nextInt();
                    link.put(k,k);
                    data[cnt.incrementAndGet()-1]=k;
                }
            }).start();
            new Thread(() -> {
                for (int j = 0; j < nRequest; j++) {
                    int k =  kk+ThreadLocalRandom.current().nextInt(nRequest);
                    link.put(k,k);
                    data[cnt.incrementAndGet()-1]=k;
                }
            }).start();
        }

        while (Thread.activeCount()>2)
            Thread.yield();

        long t1=System.nanoTime();
        System.out.println("slip time: "+(t1-star)/1000000+"ms"+" for Concurrent");

        Set<Integer> set = new HashSet<>();
        for(int i=0;i<100;i++)
            set.add(data[data.length-1-i]);
        System.out.println("---------the final map: id link last request--------unique last 100 request: "+set.size());

        for(int i=0;i<link.size();i++) {
            Integer tmp = link.getByIndex(i);
            tmp = tmp==null?-1:tmp;
            int j=0;
            int tmpd=data[data.length-1-i];
            for(;j<link.size();j++) {
                Integer tmpl = link.getByIndex(j);
                tmpl = tmpl==null?-1:tmpl;
                if (tmpd == tmpl + 0)
                    break;
            }
            System.out.println("link id " + i + ",value=" + tmp + ",last request= " + data[data.length - 1 - i] + " "
                    + (tmp == data[data.length - 1 - i])
                    + " "+j);
        }
        System.out.println(link);
    }
    static void testPutUniq(){
        LRUcache<Integer, Integer> link = new LRUcache<>(100);
        long star = System.nanoTime();
        for(int i=0;i<100;i++)
            link.put(i,i);
        int []data=new int[1000000];
        AtomicInteger cnt = new AtomicInteger();
        //用1000个线程：查阅、放入数据，模拟LRU缓存
        for (int i = 0; i < 1000; i++) {
            int kk = i;
            new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    int k = kk*1000+j;
                    link.put(k,k);
                    data[cnt.incrementAndGet()-1]=k;

                }
            }).start();
        }
        while (Thread.activeCount()>2)
            Thread.yield();

        System.out.println("slip time: "+(System.nanoTime()-star)/1000000+"ms"+" ");
        System.out.println(link);
        for(int i=0;i<link.size();i++)
            System.out.print("key="+data[data.length-i-1]+",0 ");
        System.out.println();

        System.out.println("---------the final map: id link last--------");
        for(int i=0;i<link.size();i++) {
            int tmp = link.getByIndex(i);
            int j=0;
            for(;j<data.length;j++){
                if(data[data.length-j-1]==tmp)
                    break;
            }
            System.out.println("id "+ i + ",value=" + tmp + ",last= "+j);
        }
        System.out.println();
        System.out.println("---------the final Request: id last link------");
        for(int i=0;i<link.size();i++) {
            int tmp = data[data.length-i-1];
            int j=0;
            for(;j<link.size();j++){
                if(link.getByIndex(j)==tmp)
                    break;
            }
            System.out.println("last "+ i + ",value=" + tmp + ",link= "+j);
        }
    }
    static void testPutOver(){
        LRUcache<Integer, Integer> link = new LRUcache<>(100);
        long star = System.nanoTime();
        for(int i=0;i<100;i++)
            link.put(i,i);
        int []data=new int[1000000];
        AtomicInteger cnt = new AtomicInteger();
        //用1000个线程：查阅、放入数据，模拟LRU缓存
        for (int i = 0; i < 1000; i++) {
            new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    int k = ThreadLocalRandom.current().nextInt(200) ;
                    link.put(k,k);
                    data[cnt.incrementAndGet()-1]=k;

                }
            }).start();
        }
        while (Thread.activeCount()>2)
            Thread.yield();

        System.out.println("slip time: "+(System.nanoTime()-star)/1000000+"ms"+" ");
        System.out.println(link);
        for(int i=0;i<link.size();i++)
            System.out.print("key="+data[data.length-i-1]+",0 ");
        System.out.println();

        int k=0;
        System.out.println("---------the final map: id link last--------");
        for(int i=0;i<link.size();i++) {
            Integer tmp = link.getByIndex(i);
            tmp=(tmp==null?-1:tmp);
            int j=0;
            for(;j<data.length;j++){
                if(data[data.length-j-1]==tmp) {
                    if(j<link.size())
                        k++;
                    break;
                }
            }
            System.out.println("id "+ i + ",key=" + tmp + ",last= "+j);
        }
        System.out.println();
        System.out.println("---------the final Request: id last link------last 100 request in map "+k);
        for(int i=0;i<link.size();i++) {
            Integer tmp = link.getByIndex(i);
            tmp=(tmp==null?-1:tmp);
            int j=0;
            for(;j<link.size();j++){
                if(link.getByIndex(j)==tmp)
                    break;
            }
            System.out.println("last "+ i + ",key=" + tmp + ",link= "+j);
        }
    }
    static void testGet() {
        LRUcache<Integer, Integer> link = new LRUcache<>(100);
        long star = System.nanoTime();
        for(int i=0;i<100;i++)
            link.put(i,i);
        int []data=new int[1000000];
        AtomicInteger cnt = new AtomicInteger();
        //用1000个线程：查阅、放入数据，模拟LRU缓存
        for (int i = 0; i < 1000; i++) {
            new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    int k = ThreadLocalRandom.current().nextInt(100) ;
                    data[cnt.incrementAndGet()-1]=k;
                    link.get(k);
                }
            }).start();
        }
        while (Thread.activeCount()>2)
            Thread.yield();

        System.out.println("slip time: "+(System.nanoTime()-star)/1000000+"ms"+" ");
        System.out.println(link);

        Set<Integer> set = new HashSet<>();
        for(int i=0;i<link.size();i++) {
            int tmp = data[data.length-i-1];
            set.add(tmp);
        }
        System.out.println("---------the last 100 unique request "+set.size());
        int k=0;

        int temp[] = new int[link.size()];
        for(int i=0;i<link.size();i++) {
            Integer tmp = link.getByIndex(i);
            tmp=(tmp==null?-1:tmp);
            int j=0;
            for(;j<data.length;j++){
                if(data[data.length-j-1]==tmp) {
                    if(j<link.size())
                        k++;
                    break;
                }
            }
            temp[i]=j;
        }
        System.out.println("last 100 request in map "+k);
        System.out.println("---------the final map: id link last--------");
        for(int i=0;i<temp.length;i++)
            System.out.println("id "+ i + ",key=" + link.get(i) + ",last= "+temp[i]);
        System.out.println();
        System.out.println("---------the final Request: id last link------");

        for(int i=0;i<link.size();i++) {
            int tmp = data[data.length-i-1];
            int j=0;
            for(;j<link.size();j++){
                if(link.getByIndex(j)==tmp)
                    break;
            }
            System.out.println("last "+ i + ",key=" + tmp + ",link= "+j);
        }
    }
    static void testFistTail() {
        LRUcache<Integer, Integer> link = new LRUcache<>(100);
        AtomicInteger cnt = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(1);
        long star = System.nanoTime();

        for (int i = 0; i < 10000; i++) {
            new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    link.addFirst(j,j);
                }
            }).start();
            new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    Node node = link.removeTail();
                    if (node==null){
                        cnt.incrementAndGet();
                        try{latch.await(1, TimeUnit.MILLISECONDS);}
                        catch (Exception e){}
                        link.removeTail();
                    }
                }
            }).start();
        }
        while (Thread.activeCount()>2)
            Thread.yield();

        System.out.println("slip time: "+(System.nanoTime()-star)/1000000+"ms");
        System.out.println("random rm: "+cnt.get());

        System.out.println(link);
    }
    static void testRemoveTail(){
        for (int i = 0; i < 1000; i++) {
            new Thread(() -> {
            }).start();
        }
        LRUcache<Integer, Integer> link = new LRUcache<>(100);
        long star = System.nanoTime();
        //用100万
        for (int j = 0; j < 1000000; j++)
            link.addFirst(j,j);

        long t1 = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            new Thread(() -> {
            }).start();
        }
        long t2 = System.nanoTime();

        for (int i = 0; i < 1000; i++) {
            new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    link.removeTail();
                }
            }).start();
        }


        while (Thread.activeCount()>2)
            Thread.yield();

        long t3 = System.nanoTime();

        System.out.println("single write 1000x1000 slip time: "+(t1-star)/1000000+"ms");

        System.out.println("create 1000 threads time: "+(t2-t1)/1000000+"ms");

        System.out.println("multi remove 1000x1000 slip time: "+(t3-t2-t2+t1)/1000000+"ms\n");

        System.out.println(link);
    }
}
