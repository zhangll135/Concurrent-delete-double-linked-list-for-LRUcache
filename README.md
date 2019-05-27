# Concurrent-delete-double-linked-list-for-LRUcache
Hight concurrent LRUcache while get/put in O(1)

    在阅读掘金上一篇关于mysql运行原理文章https://juejin.im/book/5bffcbc9f265da614b11b731/section第十九节 缓存池时，作者提到LRU缓存的概念，突然勾起了我以前手写LRU缓存时没有考虑并发并引以为憾的旧事，于是我发了一番功夫较劲地研究一番，最终另辟蹊径找到了解决方案—-采用联合作战的cas思想实现双链表随机并发节点删除，从而使得基于该链表构建的LRU缓存可以在复杂度为O(1)的情况下实现高并发。
    该缓存通过了以下测试：
        1、链表并发随机删除(删除远快于插入)
        2、LRU缓存的FIFO淘汰策略(100%)
        3、LRU缓存get并发性能测试(3-5倍)
        4、LRU缓存put并发性能测试(2-3倍)
	5、LRU缓存delete并发性能测试(模拟秒杀)
    以上测试均在i5-2450m 2.5G双核四线程cpu上完成，现将代码上传，如有不当之处望指出。