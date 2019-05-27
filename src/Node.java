import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
/*一个存放key，value的节点
*      可以实现CAS更新节点的前驱和后驱：casPre(old,new),casNext(old,new)
*      一个标记：delete: 0正常，1删除中，2已删除
*      补充一个标记：helper其意义由用户确定
*      */
public class Node<K,V> {
    public final K key;
    public final V value;
    volatile Node<K, V> pre, next;
    volatile int delete;
    volatile long helper;

    static AtomicReferenceFieldUpdater<Node, Node> tmppre = AtomicReferenceFieldUpdater
            .newUpdater(Node.class, Node.class, "pre");
    static AtomicReferenceFieldUpdater<Node, Node> tmpnext = AtomicReferenceFieldUpdater
            .newUpdater(Node.class, Node.class, "next");
    static AtomicIntegerFieldUpdater tmpdelete=AtomicIntegerFieldUpdater.newUpdater(Node.class,"delete");
    static AtomicLongFieldUpdater tmphelp=AtomicLongFieldUpdater.newUpdater(Node.class,"helper");

    public Node(K key,V value){
        this.key=key;
        this.value=value;
    }
    @Override public String toString(){
        return value.toString();
    }

    public boolean casPre(Node<K, V> old, Node<K, V> expect) {
        return tmppre.compareAndSet(this, old, expect);
    }
    public boolean casNext(Node<K, V> old, Node<K, V> expect) {
        return tmpnext.compareAndSet(this, old, expect);
    }
    public boolean casDelete(Integer old,Integer expect){
        return tmpdelete.compareAndSet(this,old,expect);
    }
    public boolean casHelper(long old,long expect){
        return tmphelp.compareAndSet(this,old,expect);
    }
}
