package org.openmolecules.render;

import com.actelion.research.chem.Coordinates;

import java.util.ArrayList;
import java.util.Arrays;

public class CachingMoleculeBuilder implements MoleculeBuilder {
	private final MoleculeBuilder mRenderer;
	private final ArrayList<CachedSphere> mSphereList;
	private final ArrayList<CachedCylinder> mCylinderList;
	private final ArrayList<CachedCone> mConeList;

	public CachingMoleculeBuilder(MoleculeBuilder renderer) {
		mRenderer = renderer;
		mSphereList = new ArrayList<CachedSphere>();
		mCylinderList = new  ArrayList<CachedCylinder>();
		mConeList = new  ArrayList<CachedCone>();
		}

	@Override
	public void init() {
		mSphereList.clear();
		mCylinderList.clear();
		mConeList.clear();
		}

	@Override
	public void addAtomSphere(int role, Coordinates c, double radius, int argb) {
		mSphereList.add(new CachedSphere(role, c, radius, argb));
		}

	@Override
	public void addBondCylinder(int role, double radius, double length, Coordinates c, double rotationY, double rotationZ, int argb) {
		mCylinderList.add(new CachedCylinder(role, radius, length, c, rotationY, rotationZ, argb));
		}

	@Override
	public void addAtomCone(int role, double radius, double length, Coordinates c, double rotationY, double rotationZ, int argb) {
		mConeList.add(new CachedCone(role, radius, length, c, rotationY, rotationZ, argb));
	}

	@Override
	public void done() {
		CachedCylinder[] cylinders = mCylinderList.toArray(new CachedCylinder[0]);
		Arrays.sort(cylinders);
		for (CachedCylinder c:cylinders)
			mRenderer.addBondCylinder(c.role, c.radius, c.length, c.c, c.rotationY, c.rotationZ, c.argb);

		CachedSphere[] spheres = mSphereList.toArray(new CachedSphere[0]);
		Arrays.sort(spheres);
		for (CachedSphere s:spheres)
			mRenderer.addAtomSphere(s.role, s.c, s.radius, s.argb);
		}

	private class CachedSphere implements Comparable<CachedSphere> {
		int role,argb;
		Coordinates c;
		double radius;

		public CachedSphere(int role, Coordinates c, double radius, int argb) {
			this.role = role;
			this.c = new Coordinates(c);
			this.radius = radius;
			this.argb = argb;
			}

		@Override
		public int compareTo(CachedSphere o) {
			return (argb == o.argb) ? 0 : (argb < o.argb) ? -1 : 1;
			}
		}

	private class CachedCylinder implements Comparable<CachedCylinder> {
		int role,argb;
		Coordinates c;
		double radius,length,rotationY,rotationZ;

		public CachedCylinder(int role, double radius, double length, Coordinates c, double rotationY, double rotationZ, int argb) {
			this.role = role;
			this.radius = radius;
			this.length = length;
			this.c = new Coordinates(c);
			this.rotationY = rotationY;
			this.rotationZ = rotationZ;
			this.argb = argb;
			}

		@Override
		public int compareTo(CachedCylinder o) {
			return (argb == o.argb) ? 0 : (argb < o.argb) ? -1 : 1;
			}
		}

	private class CachedCone implements Comparable<CachedCone> {
		int role,argb;
		Coordinates c;
		double radius,length,rotationY,rotationZ;

		public CachedCone(int role, double radius, double length, Coordinates c, double rotationY, double rotationZ, int argb) {
			this.role = role;
			this.radius = radius;
			this.length = length;
			this.c = new Coordinates(c);
			this.rotationY = rotationY;
			this.rotationZ = rotationZ;
			this.argb = argb;
			}

		@Override
		public int compareTo(CachedCone o) {
			return (argb == o.argb) ? 0 : (argb < o.argb) ? -1 : 1;
		}
		}
	}
