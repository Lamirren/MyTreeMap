import java.util.concurrent.atomic.AtomicReference;

class Node<K, V> {
    K key;
    V value;
    AtomicReference<Node<K, V>> left = new AtomicReference<>(null); // Ссылка на левого "брата"
    AtomicReference<Node<K, V>> right = new AtomicReference<>(null); // Ссылка на правого "брата"
    Node<K, V> parent; // Ссылка на родительский узел
    boolean isRed; // Флаг, указывающий, является ли узел красным

    Node(K key, V value, boolean isRed) {
        this.key = key;
        this.value = value;
        this.isRed = isRed;
    }
}

