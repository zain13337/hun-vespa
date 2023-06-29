// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#pragma once

#include <vespa/vespalib/util/size_literals.h>
#include <utility>
#include <cstddef>

namespace vespalib::alloc {

class PtrAndSize {
public:
    PtrAndSize() noexcept : _ptr(nullptr), _sz(0ul) {}
    PtrAndSize(void * ptr, size_t sz) noexcept;
    void * get() const noexcept { return _ptr; }
    size_t size() const noexcept { return _sz; }
private:
    void * _ptr;
    size_t _sz;
};

/*
 * Abstract base class for allocating memory at a low level.
 */
class MemoryAllocator {
public:
    static constexpr size_t PAGE_SIZE = 4_Ki;
    static constexpr size_t HUGEPAGE_SIZE = 2_Mi;
    MemoryAllocator(const MemoryAllocator &) = delete;
    MemoryAllocator & operator = (const MemoryAllocator &) = delete;
    MemoryAllocator() noexcept = default;
    virtual ~MemoryAllocator() = default;
    virtual PtrAndSize alloc(size_t sz) const = 0;
    virtual void free(PtrAndSize alloc) const noexcept = 0;
    // Allow for freeing memory there size is the size requested, and not the size allocated.
    virtual void free(void * ptr, size_t sz) const noexcept {
        free(PtrAndSize(ptr, sz));
    }
    /*
     * If possible the allocations will be resized. If it was possible it will return the real size,
     * if not it shall return 0.
     * Afterwards you have a buffer that can be accessed up to the new size.
     * The old buffer is unmodified up to the new size.
     * This is thread safe and at no point will data in the buffer be invalid.
     * @param newSize The desired new size
     * @return true if successful.
     */
    virtual size_t resize_inplace(PtrAndSize current, size_t newSize) const = 0;
    static size_t roundUpToHugePages(size_t sz) noexcept {
        return (sz+(HUGEPAGE_SIZE-1)) & ~(HUGEPAGE_SIZE-1);
    }
    static const MemoryAllocator * select_allocator();
    static const MemoryAllocator * select_allocator(size_t mmapLimit, size_t alignment);
};

}
