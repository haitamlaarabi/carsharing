package org.matsim.haitam.api.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Matrix<R, C, V> {

	HashMap<R, HashMap<C, V>> m;
	MatrixInit<V> init;
	
	public Matrix() {
		this.m = new HashMap<R, HashMap<C, V>>();
	}
	
	public Matrix(Collection<R> rows, Collection<C> cols, MatrixInit<V> init) {
		this();
		this.init = init;
		initialize(rows, cols, this.init);
	}
	
	public void initialize(Collection<R> rows, Collection<C> cols, MatrixInit<V> init) {
		this.m.clear();
		for(R r : rows) {
			this.m.put(r, new HashMap<C, V>());
			for(C c : cols) {
				this.m.get(r).put(c, init.getInstance());
			}
		}
	}
	
	public void initialize() {
		for(R r : this.m.keySet()) {
			for(C c : this.m.get(r).keySet()) {
				this.m.get(r).put(c, init.getInstance());
			}
		}
	}
	
	public void set(R row) {
		this.m.put(row, new HashMap<C, V>());
	}
		
	public void set(R row, C col, V val) {
		if(!this.m.containsKey(row)) {
			this.set(row);
		}
		this.m.get(row).put(col, val);
	}
	
	public V get(R row, C col) {
		if(!this.m.containsKey(row)) {
			throw new RuntimeException("Non existing row: " + row);
		}
		if(!this.m.get(row).containsKey(col)) {
			throw new RuntimeException("Non existing col: " + col);
		}
		return this.m.get(row).get(col);
	}
	
	public Set<R> rowsSet() {
		return this.m.keySet();
	}
	
	public Set<C> colsSet(R row) {
		if(!this.m.containsKey(row)) {
			throw new RuntimeException("Non existing row: " + row);
		}
		return this.m.get(row).keySet();
	}
	
	public Map<C, V> colsMap(R row) {
		if(!this.m.containsKey(row)) {
			throw new RuntimeException("Non existing row: " + row);
		}
		return this.m.get(row);
	}
	
	public Collection<V> values(R row) {
		if(!this.m.containsKey(row)) {
			throw new RuntimeException("Non existing row: " + row);
		}
		return this.m.get(row).values();
	}
	
	public boolean isEmpty() {
		return this.m.isEmpty();
	}
	
	public void clear() {
		this.m.clear();
	}
	
	/*public Collection<Entry<R,V>> diagonal() {
		ArrayList<Entry<R,V>> d = new ArrayList<Entry<R,V>>();
		for(R t : this.m.keySet()) {
			d.add(new AbstractMap.SimpleEntry<R, V>(t, this.m.get(t).get(t)));
		}
		return d;
	}*/
	
	public interface MatrixInit<V> {
		V getInstance();
	}
	
	/*public double[][] build() {
		double[][] o = new double[this.m.keySet().size()][this.m.keySet().size()];
		int i = 0;
		for(T t1 : this.m.keySet()) {
			int j = 0;
			for(T t2 : this.m.get(t1).keySet()) {
				o[i][j] = (Double) this.m.get(t1).get(t2);
				j++;
			}
			i++;
		}
		return o;
	}
	
	public String header() {
		String toprint = "";
		for(T y : this.m.keySet()) {
			toprint += y.toString() + "\t";
		}
		return toprint;
	}*/
	
	@Override
	public String toString() {
		String toprint = "";
		for(R y : this.m.keySet()) {
			toprint += "\t" + y.toString();
		}
		for(R y : this.m.keySet()) {
			toprint += "\n" + y;
			for(C x : this.m.get(y).keySet()) {
				toprint += "\t" + this.m.get(y).get(x).toString() ;
			}
		}
		return toprint;
	}
	
	public ArrayList<String> toStringList() {
		ArrayList<String> lstr = new ArrayList<String>();
		String header = "";
		for(R r : this.m.keySet()) {
			String row = r.toString();
			for(C c : this.m.get(r).keySet()) {
				if(lstr.isEmpty())	header += "\t" + c.toString();
				row += "\t" + this.m.get(r).get(c).toString();
			}
			lstr.add(row);
		}
		lstr.add(0, header);
		return lstr;
	}
	
}
