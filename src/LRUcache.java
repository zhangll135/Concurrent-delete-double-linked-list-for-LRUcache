import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
/*实现LRU缓存：
 *       可以实现缓存查询：V value=get(K key)
 *       可以实现缓存写入：boolean put(K key,V value)
 *       可以并发缓存删除：boolean delete(K key)
 *       当缓存满时,淘汰最近最少使用页
 * ------------------------------------------------------
 *底层并发删除链表：
 *     可以实现并发插入头节点数据：addFirst(K key,V value)/addFirst(Node<K,V>);
 *     可以实现并发删除尾节点数据：Node<K,V> res=removeTail();
 *     可以实现并发删除任意节点据：boolean removeNode(Node<K,V>);
 *
 *     插入、删除复杂度为O(1)*/
class LRUcache<K,V> {
    Node<K,V> head = new Node(null,null);
    Node<K,V> tail = new Node(null,null);
    int capacity;
    Map<K,Node<K,V>> map = new ConcurrentHashMap<>();
    AtomicInteger cnt = new AtomicInteger();//map的size性能太差

    public LRUcache(int cpacity){
        head.next=tail;
        tail.pre=head;
        this.capacity = cpacity;
    }
    public int size(){return cnt.get();}
    @Override public String toString(){
        StringBuilder sb= new StringBuilder().append("link:"+size()+": [");
        int i=0;
        for (Node t = head.next;  t != tail; t = t.next,i++)
            sb.append( t.key + "=" +t.value+":"+t.delete+", ");
        sb.append("\b]"+i+"\n\n");

        sb.append(" map:"+map.size()+": "+map);
        return sb.toString();
    }
    public V get(K key){
        Node<K,V> old = map.get(key);
        if(old==null)
            return null;
        //已经第1页
        if(old.pre==head||old.pre==null)
            return old.value;
        //主代码移动old到链表头部：尽快更新map
        if(old.casHelper(0,System.currentTimeMillis())) {
            removeNode(map.put(key,addFirst(key,old.value)));
            return old.value;
        }
        //old超过10秒未移除，直接添加该节点到头部
        if(System.currentTimeMillis()-old.helper>100000&&(old.casDelete(2,3)||old.casDelete(1,3)))
            removeNode(map.put(key,addFirst(key,old.value)));
        return old.value;
    }
    public boolean put(K key,V value){
        Node<K,V> node = new Node<>(key,value);
        Node<K,V>old=map.get(key);
        //替换原来页
        if(old!=null){
            if(!old.casHelper(0,System.currentTimeMillis()))
                return false;
            removeNode(map.put(key,addFirst(key,old.value)));
            return true;
        }
        //放入新页
        if(map.putIfAbsent(key,node)!=null)
            return false;
        addFirst(node);
        //淘汰尾页
        if(cnt.get()>=capacity&&(old=removeTail())!=null) {
            removeNode(map.remove(old.key));
            return true;
        }
        cnt.incrementAndGet();
        return true;
    }
    public V delete(K key){
        //该key不再使用
        Node<K,V>node=map.remove(key);
        if (removeNode(node)) {
            cnt.decrementAndGet();
            return node.value;
        }
        return null;
    }
    public void clear(){
        tail.pre=head;
        head.next=tail;
        map.clear();
        cnt.set(0);
    }

    Node<K,V> addFirst(K key,V value){
        return addFirst(new Node<>(key,value));
    }
    Node<K,V> addFirst(Node<K,V>res){
        for(res.pre=res.next=null;;){
            //获取第1节点并助其状态完整
            Node<K,V>headnext = head.next;
            if(headnext.pre==null){
                Node<K,V> headnextnext = headnext.next;
                headnextnext.casPre(head, headnext);
                headnext.casPre(null,head);
            }
            //cas尝试抢head指针
            res.next=headnext;
            if (head.casNext(headnext,res)) {
                headnext.casPre(head,res);
                res.casPre(null,head);
                return res;
            }
        }
    }
    boolean removeNode(Node<K,V> node) {
        if (node == null || !node.casDelete(0, 1))
            return false;
        //删除主线程
        if (node.pre == null)
            return true;
        for (Node<K, V> vtail = node.next; ; ) {
            //向后找到虚拟尾节点：vtail
            while (vtail.delete != 0)
                vtail = vtail.next;
            Node<K, V> vtailpre = vtail.pre;
            Node<K, V> vtailprepre = vtailpre == null ? null : vtailpre.pre;
            //1 删除节点是第一页：保留等待后来的节点删除它
            if ((vtailpre == head || vtailprepre == head || vtailprepre == null) && vtail.delete == 0)
                return true;
            //2 该node事实上已经进入死亡区：vtailprepre已经跑到node前面
            if ((node.pre.delete == 2 || vtailpre.delete == 0))
                node.delete = 2;
            //3 唯一能确认该节点真正死亡的不可变条件：delete==2
            if (node.delete == 2)
                return true;
            //4 唯一能cas删除节点的操作：vtail的步步前推
            if (vtail.delete == 0 && vtail.casPre(vtailpre, vtailprepre)) {
                vtail.pre.next = vtail;
                if (vtail.delete == 0)
                    vtailpre.delete = 2;
            }
        }
    }
    Node<K,V> removeTail(){//更新tail指针：找到第一个可删除的节点并删除
        Node<K,V> node = tail.pre;
        while (node.pre!=head&&node.pre!=null){//node.casHelper抢删除权限
            if(node.delete==0&&node.casHelper(0,System.currentTimeMillis())&&removeNode(node))
                return node;
            //tail指针cas更新：只能前进
            tail.casPre(node,node.pre);
            tail.pre.next=tail;
            node=tail.pre;
        }
        //删除首节点
        if(node!=head&&removeNode(node))
            return node;
        //前面再无节点可删除
        return null;
    }
    V getByIndex(int i){// 测试用，线程不安全
        Node<K,V>res = head.next;
        while (res != null && i-- > 0)
            res = res.next;
        return res==null?null:res.value;
    }

}
