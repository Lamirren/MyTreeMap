import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MyTreeMap<K, V> {
    // Основная идея этой реализации в счетчике модификаций, используя который мы можем обходить блокировки и ошибки при чтении/вставке
    // Однако это все работает только при одном но - низкой нагрузке
    private AtomicInteger modificationCount = new AtomicInteger(0); // Счетчик модификаций
    private AtomicReference<Node<K, V>> root = new AtomicReference<>(null);

    // Дальше идут методы со *, так как могут иметь (имеют) ограничения из-за особенностей многопоточной работы
    // и требуют либо обертки либо неиспользования (дописывания) хD

    // Рекурсивно считаем количество узлов для метода size
    private int sizeRecursive(Node<K, V> node) {
        if (node == null) {
            return 0;
        }
        return 1 + sizeRecursive(node.left.get()) + sizeRecursive(node.right.get());
    }

    // Считаем количество узлов, пока нет изменений, в противном случае пересчитываем
    public int size() {
        int observedModCount;
        int size;

        do {
            observedModCount = modificationCount.get(); // Запоминаем значение счетчика перед подсчетом
            size = sizeRecursive(root.get());
        } while (observedModCount != modificationCount.get()); // Проверяем, изменился ли счетчик

        return size;
    }

    // Вызывается при каждой модификации дерева
    private void incrementModificationCount() {
        modificationCount.incrementAndGet();
    }

    // Основной метод для вставки ключа и значения
    public V put(K key, V value) {
        if (root.get() == null) {
            root.set(new Node<>(key, value, false)); // Создаем корневой узел как черный
            return null;
        }

        Node<K, V> insertedNode = insertRecursive(root.get(), key, value, null);
        insertFixUp(insertedNode); // Корректируем дерево после вставки
        root.set(insertedNode);
        root.get().isRed = false; // Убедиться, что корень остается черным

        return null; // Можно изменить
    }

    // Здесь чинятся узлы (перекрашиваются, переворачиваются)
    private void insertFixUp(Node<K, V> node) {
        while (node != null && node != root.get() && node.parent.isRed) {
            if (parentOf(node) == leftOf(parentOf(parentOf(node)))) {
                Node<K, V> y = rightOf(parentOf(parentOf(node)));
                if (isRed(y)) {
                    setColor(parentOf(node), false);
                    setColor(y, false);
                    setColor(parentOf(parentOf(node)), true);
                    node = parentOf(parentOf(node));
                } else {
                    if (node == rightOf(parentOf(node))) {
                        node = parentOf(node);
                        leftRotate(node);
                    }
                    setColor(parentOf(node), false);
                    setColor(parentOf(parentOf(node)), true);
                    rightRotate(parentOf(parentOf(node)));
                }
            } else { // Симметричный случай
                Node<K, V> y = leftOf(parentOf(parentOf(node)));
                if (isRed(y)) {
                    setColor(parentOf(node), false);
                    setColor(y, false);
                    setColor(parentOf(parentOf(node)), true);
                    node = parentOf(parentOf(node));
                } else {
                    if (node == leftOf(parentOf(node))) {
                        node = parentOf(node);
                        rightRotate(node);
                    }
                    setColor(parentOf(node), false);
                    setColor(parentOf(parentOf(node)), true);
                    leftRotate(parentOf(parentOf(node)));
                }
            }
        }
        root.get().isRed = false;
    }


    private boolean isRed(Node<K, V> node) {
        return node != null && node.isRed;
    }

    private Node<K, V> parentOf(Node<K, V> node) {
        return node != null ? node.parent : null;
    }

    private Node<K, V> leftOf(Node<K, V> node) {
        return node != null ? node.left.get() : null;
    }

    private Node<K, V> rightOf(Node<K, V> node) {
        return node != null ? node.right.get() : null;
    }

    private void setColor(Node<K, V> node, boolean isRed) {
        if (node != null) {
            node.isRed = isRed;
        }
    }

    private Node<K, V> insertRecursive(Node<K, V> current, K key, V value, Node<K, V> parent) {
        if (current == null) {
            // Создание нового узла, который станет левым или правым потомком parent
            Node<K, V> newNode = new Node<>(key, value, true); // Новые узлы красные
            newNode.parent = parent;
            return newNode;
        }

        int cmp = compare(key, current.key);
        if (cmp < 0) {
            // Вставка слева
            Node<K, V> leftChild = insertRecursive(current.left.get(), key, value, current);
            current.left.set(leftChild);
        } else if (cmp > 0) {
            // Вставка справа
            Node<K, V> rightChild = insertRecursive(current.right.get(), key, value, current);
            current.right.set(rightChild);
        } else {
            // Если ключ уже существует, обновляем значение
            current.value = value;
        }

        return current;
    }


    // Логика левого вращения
    private void leftRotate(Node<K, V> x) {
        Node<K, V> y = x.right.get(); // установить y
        x.right.set(y.left.get()); // переместить левое поддерево y в правое поддерево x
        if (y.left.get() != null) {
            y.left.get().parent = x;
        }
        y.parent = x.parent; // связать родителя x с y

        if (x.parent == null) {
            this.root.set(y); // если x - корень, то y становится корнем
        } else if (x == x.parent.left.get()) {
            x.parent.left.set(y);
        } else {
            x.parent.right.set(y);
        }
        y.left.set(x); // поместить x на левое поддерево y
        x.parent = y;
    }


    // Логика правого вращения
    private void rightRotate(Node<K, V> y) {
        Node<K, V> x = y.left.get(); // установить x
        y.left.set(x.right.get()); // переместить правое поддерево x в левое поддерево y
        if (x.right.get() != null) {
            x.right.get().parent = y;
        }
        x.parent = y.parent; // связать родителя y с x

        if (y.parent == null) {
            this.root.set(x); // если y - корень, то x становится корнем
        } else if (y == y.parent.right.get()) {
            y.parent.right.set(x);
        } else {
            y.parent.left.set(x);
        }
        x.right.set(y); // поместить y на правое поддерево x
        y.parent = x;
    }


    // Инвертирование цветов
    private void colorFlip(Node<K, V> node) {

    }

    // Сравнение 2 ключей
    private int compare(K key1, K key2) {
        return ((Comparable<K>) key1).compareTo(key2);
    }

    public V get(K key) {
        int observedModCount;

        do {
            observedModCount = modificationCount.get(); // Запоминаем значение счетчика перед поиском
            Node<K, V> node = getNode(key);
            if (node != null) {
                return node.value;
            }
        } while (observedModCount != modificationCount.get()); // Проверяем, изменился ли счетчик

        return null; // Ключ не найден или дерево изменилось в процессе поиска
    }

    private Node<K, V> getNode(K key) {
        Node<K, V> current = root.get();

        while (current != null) {
            int cmp = compare(key, current.key);

            if (cmp == 0) {
                return current; // Найден соответствующий ключ
            } else if (cmp < 0) {
                current = current.left.get(); // Переход к левому поддереву
            } else {
                current = current.right.get(); // Переход к правому поддереву
            }
        }

        return null; // Ключ не найден
    }

    // Эффективно (или в целом работает), если нет конкуренции
    public void clear() {
        // Повторять пока операция не будет успешной
        while (true) {
            Node<K, V> currentRoot = root.get();

            // Проверяем, не изменилось ли дерево в процессе выполнения операции
            if (root.compareAndSet(currentRoot, null)) {
                // Успешно установили корень в null, очистка завершена
                return;
            }
            // В противном случае, повторяем попытку, так как дерево изменилось
        }
    }

    // Ищем в дереве есть ли такой ключ
    public boolean containsKey(K key) {
        return getNode(key) != null;
    }

    // Ищем в дереве есть ли такое значение
    public boolean containsValue(V value) {
        return containsValueRecursive(root.get(), value);
    }

    // Рекурсивный поиск значения
    private boolean containsValueRecursive(Node<K, V> node, V value) {
        if (node == null) {
            return false;
        }
        if (value.equals(node.value)) {
            return true;
        }
        return containsValueRecursive(node.left.get(), value) || containsValueRecursive(node.right.get(), value);
    }

    // Проверка на пустоту дерева
    public boolean isEmpty() {
        return root.get() == null;
    }
}
