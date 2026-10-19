
```java
public interface Buffer {
    DataBuffer cache();
    int capacity();
}
public interface GpuBuffer {
    long handle();
    Flag<Role> roles();
    Flag<MemoryProp> memProps();
}
```

### Resizing a static buffer

Device local. No permanent host cache.

1. Create a new larger buffer.
2. Invoke a copy command to transfer current data (async)
3. Invoke a copy command to transfer new data per standard update (async, must happen after previous copy).

### Resizing a read/write dynamic buffer

Device local. Permanent host cache.

1. Create a new larger device buffer.
2. Create a new larger host buffer.
3. Invoke a copy command to transfer from old device buffer to new device buffer (async).
4. Copy from old host buffer to new host buffer.
5. Invoke copy commands per standard update (async, must happen after previous device copy)

### Resizing a write-only dynamic buffer

Device local. Permanent host cache.

1. Create a new larger device buffer.
2. Create a new larger host buffer.
3. Invoke a copy command to transfer from old device buffer to new device buffer (async).
4. Invoke copy commands per standard update (async, must happen after previous device copy)

### Resizing a coherent streaming buffer

* Host visible
* Buffer per frame-in-flight.
* Data is coherent across frames.

Direct on resize: create new larger host buffer; copy from old host buffer to new host buffer.

For each frame, before the current buffer's cache is accessed, if the current buffer is too small:

1. Create a new larger device buffer.
2. Copy all data from previous buffer to this buffer (async).

### Resizing an incoherent streaming buffer

* Host visible
* Buffer per frame-in-flight.
* No separate host buffer.

For each frame, before the current buffer's cache is access, if the current buffer is too small: make a new larger buffer.
