package org.antlr.v4.runtime.misc;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/** Set impl with closed hashing (open addressing). */
public class Array2DHashSet<T> implements Set<T> {

	public static final int INITAL_CAPACITY = 16; // must be power of 2
	public static final int INITAL_BUCKET_CAPACITY = 8;
	public static final double LOAD_FACTOR = 0.75;

	protected T[][] buckets;

	/** How many elements in set */
	protected int n = 0;

	protected int threshold = (int)(INITAL_CAPACITY * LOAD_FACTOR); // when to expand

	protected int currentPrime = 1; // jump by 4 primes each expand or whatever
	protected int initialBucketCapacity = INITAL_BUCKET_CAPACITY;

	public Array2DHashSet() {
		this(INITAL_CAPACITY, INITAL_BUCKET_CAPACITY);
	}

	public Array2DHashSet(int initialCapacity, int initialBucketCapacity) {
		buckets = (T[][])new Object[initialCapacity][];
		this.initialBucketCapacity = initialBucketCapacity;
	}

	/** Add o to set if not there; return existing value if already there. */
	public T put(T o) {
		if ( n > threshold ) expand();
		return put_(o);
	}

	protected T put_(T o) {
		int b = getBucket(o);
		T[] bucket = buckets[b];
		// NEW BUCKET
		if ( bucket==null ) {
			buckets[b] = (T[])new Object[initialBucketCapacity];
			buckets[b][0] = o;
			n++;
			return o;
		}
		// LOOK FOR IT IN BUCKET
		for (int i=0; i<bucket.length; i++) {
			T existing = bucket[i];
			if ( existing==null ) { // empty slot; not there, add.
				bucket[i] = o;
				n++;
				return o;
			}
			if ( equals(existing,o) ) return existing; // found existing, quit
		}
		// FULL BUCKET, expand and add to end
		T[] old = bucket;
		bucket = (T[])new Object[old.length * 2];
		buckets[b] = bucket;
		System.arraycopy(old, 0, bucket, 0, old.length);
		bucket[old.length] = o; // add to end
		n++;
		return o;
	}

	public T get(T o) {
		if ( o==null ) return o;
		int b = getBucket(o);
		T[] bucket = buckets[b];
		if ( bucket==null ) return null; // no bucket
		for (T e : bucket) {
			if ( e==null ) return null; // empty slot; not there
			if ( equals(e,o) ) return e;
		}
		return null;
	}

	protected int getBucket(T o) {
		int hash = hashCode(o);
		int b = hash & (buckets.length-1); // assumes len is power of 2
		return b;
	}

	@Override
	public int hashCode() {
		int h = 0;
		for (T[] bucket : buckets) {
			if ( bucket==null ) continue;
			for (T o : bucket) {
				if ( o==null ) break;
				h += hashCode(o);
			}
		}
		return h;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if ( !(o instanceof Array2DHashSet) || o==null ) return false;
		Array2DHashSet<T> other = (Array2DHashSet<T>)o;
		if ( other.size() != size() ) return false;
		boolean same = this.containsAll(other) && other.containsAll(this);
		return same;
	}

	protected void expand() {
		T[][] old = buckets;
		currentPrime += 4;
		int newCapacity = buckets.length * 2;
		T[][] newTable = (T[][])new Object[newCapacity][];
		buckets = newTable;
		threshold = (int)(newCapacity * LOAD_FACTOR);
//		System.out.println("new size="+newCapacity+", thres="+threshold);
		// rehash all existing entries
		int oldSize = size();
		for (T[] bucket : old) {
			if ( bucket==null ) continue;
			for (T o : bucket) {
				if ( o==null ) break;
				put_(o);
			}
		}
		n = oldSize;
	}

	public int hashCode(T o) {
		return o.hashCode();
	}

	public boolean equals(T a, T b) {
//		if ( a==b ) return true;
//		if ( a==null || b==null ) return false;
//		if ( hashCode(a) != hashCode(b) ) return false;
		return a.equals(b);
	}

	@Override
	public boolean add(T t) {
		T existing = put(t);
		return existing==t;
	}

	@Override
	public int size() {
		return n;
	}

	@Override
	public boolean isEmpty() {
		return n==0;
	}

	@Override
	public boolean contains(Object o) {
		return get((T)o) != null;
	}

	@Override
	public Iterator<T> iterator() {
//		return new Iterator<T>() {
//			int i = -1;
//			@Override
//			public boolean hasNext() { return (i+1) < table.length; }
//
//			@Override
//			public T next() {
//				i++;
//				if ( i > table.length ) throw new NoSuchElementException();
//				while ( table[i]==null ) i++;
//				return table[i];
//			}
//
//			@Override
//			public void remove() {
//			}
//		}
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] toArray() {
		Object[] a = new Object[size()];
		int i = 0;
		for (T[] bucket : buckets) {
			if ( bucket==null ) continue;
			for (T o : bucket) {
				if ( o==null ) break;
				a[i++] = o;
			}
		}
		return a;
	}

	@Override
	public <U> U[] toArray(U[] a) {
		int i = 0;
		for (T[] bucket : buckets) {
			if ( bucket==null ) continue;
			for (T o : bucket) {
				if ( o==null ) break;
				a[i++] = (U)o;
			}
		}
		return a;
	}

	@Override
	public boolean remove(Object o) {
		if ( o==null ) return false;
		int b = getBucket((T)o);
		T[] bucket = buckets[b];
		if ( bucket==null ) return false; // no bucket
		for (int i=0; i<bucket.length; i++) {
			T e = bucket[i];
			if ( e==null ) return false;  // empty slot; not there
			if ( equals(e,(T)o) ) {          // found it
				// shift all elements to the right down one
//				for (int j=i; j<bucket.length-1; j++) bucket[j] = bucket[j+1];
				System.arraycopy(bucket, i+1, bucket, i, bucket.length-i-1);
				n--;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> collection) {
		if ( collection instanceof Array2DHashSet ) {
			Array2DHashSet<T> s = (Array2DHashSet<T>)collection;
			for (T[] bucket : s.buckets) {
				if ( bucket==null ) continue;
				for (T o : bucket) {
					if ( o==null ) break;
					if ( !this.contains(o) ) return false;
				}
			}
		}
		else {
			for (Object o : collection) {
				if ( !this.contains(o) ) return false;
			}
		}
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		boolean changed = false;
		for (T o : c) {
			T existing = put(o);
			if ( existing!=o ) changed=true;
		}
		return changed;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		buckets = (T[][])new Object[INITAL_CAPACITY][];
		n = 0;
	}

	public String toString() {
		if ( size()==0 ) return "{}";

		StringBuilder buf = new StringBuilder();
		buf.append('{');
		boolean first = true;
		for (T[] bucket : buckets) {
			if ( bucket==null ) continue;
			for (T o : bucket) {
				if ( o==null ) break;
				if ( first ) first=false;
				else buf.append(", ");
				buf.append(o.toString());
			}
		}
		buf.append('}');
		return buf.toString();
	}

	public String toTableString() {
		StringBuilder buf = new StringBuilder();
		for (T[] bucket : buckets) {
			if ( bucket==null ) {
				buf.append("null\n");
				continue;
			}
			buf.append('[');
			boolean first = true;
			for (T o : bucket) {
				if ( first ) first=false;
				else buf.append(" ");
				if ( o==null ) buf.append("_");
				else buf.append(o.toString());
			}
			buf.append("]\n");
		}
		return buf.toString();
	}

	public static void main(String[] args) {
		Array2DHashSet<String> clset = new Array2DHashSet<String>();
		Set<String> set = clset;
		set.add("hi");
		set.add("mom");
		set.add("foo");
		set.add("ach");
		set.add("cbba");
		set.add("d");
		set.add("edf");
		set.add("f");
		set.add("gab");
		set.remove("ach");
		System.out.println(set);
		System.out.println(clset.toTableString());
	}
}
