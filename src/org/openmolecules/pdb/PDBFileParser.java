package org.openmolecules.pdb;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.ExtendedMolecule;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.conf.VDWRadii;
import com.actelion.research.chem.Molecule3D;

import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * http://www.daylight.com/meetings/mug01/Sayle/m4xbondage.html
 * 
 * @author freyssj
 */
public class PDBFileParser extends AbstractParser {

	private boolean isCreateConnections = true;

	private boolean isCreateBondOrders = true;

	private boolean isLoadHeteroAtoms = true;

	private boolean isLoadHydrogen = false;

	private boolean isLoadWater = true;

	private boolean isLoadSalts = true;

	private boolean isLoadHalfOccupancy = false;

	private boolean isLenient = true;

	private boolean isKeepMainStructureOnly = false;

	private String crystal;

	private Date depositionDate;
	
	private String proteinName;

	private double resolution;
	
	private String classification;
	
	private int loadModel = -1;
	
	private String loadChain = null;
	
	private boolean pqr = false;
	
	public PDBFileParser() {}

	/**
	 * 
	 * @param res
	 * @param mol
	 * @param fileName
	 * @param modelNo
	 * @param atomToGroup
	 * @param markedLigs
	 * @throws Exception
	 */
	private void addMol(List<Molecule3D> res, Molecule3D mol, String fileName, int modelNo, Map<Integer, String> atomToGroup, List<Integer> markedLigs) throws Exception{
		if(mol==null || mol.getAllAtoms()==0) return;
		mol.setAuxiliaryInfo("isPDB", true);		
		
		if(classification!=null) mol.setAuxiliaryInfo("CLASSIFICATION", classification);
		if(proteinName!=null && proteinName.length()<30) {
			mol.setName(proteinName);
		} else {
			mol.setName(fileName);
		}
		if(modelNo>0) mol.setName(mol.getName()+" Model "+ modelNo);

		
		//Clean chains that are alternate positions
		boolean conflictFound = true;
		atomConflictLoop: while(conflictFound) {
			MoleculeGrid grid = new MoleculeGrid(mol, 1);
			conflictFound = false;			
			for (int a=0; a<mol.getAllAtoms(); a++) {
				if(mol.getAtomicNo(a)<=1) continue;
				Set<Integer> atoms = grid.getNeighbours(mol.getCoordinates(a), 1, true);
				if(atoms.size()>1) {
					//We have a conflict, we have to chose one chain
					Set<String> chains = new HashSet<String>();
					boolean canBeDeleted = false;
					for (int at : atoms) {
						if(mol.getAtomAmino(at).endsWith("A")) {
							canBeDeleted = true;
						} else {
							chains.add(mol.getAtomAmino(at));
						}
					}
					if(canBeDeleted && chains.size()>0) {
						List<Integer> toDelete = new ArrayList<Integer>();
						for (String chainId : chains) {
							System.out.println("REMOVE Conflicting chain "+chainId);
							for (int at=0; at<mol.getAllAtoms(); at++) {
								if(mol.getAtomAmino(at).equals(chainId)) {
									toDelete.add(at);
								}
							}
						}
						if(toDelete.size()>0) {
							mol.deleteAtoms(toDelete);
							conflictFound = true;
							continue atomConflictLoop;
						}
					}					
				}			
			}
		}
			
		
		
		
		//Create Connections
		if (isCreateConnections) {
			BondsCalculator.createBonds(mol, isLenient, atomToGroup);
			if (isCreateBondOrders) BondsCalculator.calculateBondOrders(mol, isLenient);

			//Delete Salts or small groups
			List<List<Integer>> groups = getConnexComponents(mol);
			//If a group consists of mixed proteins and ligands atoms (ex 1apt)
			for (int i = 0; i < groups.size(); i++) {
				List<Integer> group = groups.get(i);
				int nLigs = 0;
				int nProts = 0;
				for (int a : group) {
					if(mol.isAtomFlag(a, Molecule3D.LIGAND)) {
						nLigs++;
					} else {
						nProts++;
					}					
				}
				if (nLigs>0 && nProts>0) {
					for (int a : group) mol.setAtomFlag(a, Molecule3D.LIGAND, nLigs>nProts);
				}
			}
			

			if (!isLoadSalts()) {
				List<Integer> atomToDelete = new ArrayList<Integer>();
				for (int i = mol.getAllAtoms() - 1; i >= 0; i--) {
					if (mol.getAllConnAtoms(i) == 0 && (mol.getAtomicNo(i) != 8 || !(/*isLoadStableWater ||*/ isLoadWater) )) {
						atomToDelete.add(i);
					}
				}

				for (int i = 0; i < groups.size(); i++) {
					List<Integer> group = groups.get(i);
					if (group.size() > 1 && group.size() <= 5) {
						atomToDelete.addAll(group);
					}
				}
				Collections.sort(atomToDelete);
				for (int i = atomToDelete.size() - 1; i >= 0; i--) {
					mol.deleteAtom(atomToDelete.get(i));
				}
			}
			
		}
		
		if(markedLigs.size()>0) {
			mol.setAllAtomFlag(Molecule3D.LIGAND, false);
			for (int atom : markedLigs) mol.setAtomFlag(atom, Molecule3D.LIGAND, true);
			
			boolean connectedToProtein = false;
			for (int atom : markedLigs) {
				for (int i = 0; i < mol.getAllConnAtoms(atom); i++) {
					if(!mol.isAtomFlag(mol.getConnAtom(atom, i), Molecule3D.LIGAND)) {
						System.out.println("Ligand conencted to protein?? Reset");
						connectedToProtein = true; break;
					}
				}
			}
			if(connectedToProtein) for (int atom : markedLigs) mol.setAtomFlag(atom, Molecule3D.LIGAND, false);
		}
		

		
		if (isKeepMainStructureOnly && mol.getAllBonds() > 4000) {
			PDBFileParser.extractMainStructure(new Molecule3D(mol), mol);
		}

		// Mark Backbone
		flagBackbone(mol);
		for (int i = 0; i < mol.getAllAtoms(); i++) {
			String n = mol.getAtomName(i);
			if (n != null && (n.equals("CA") || n.equals("C") || n.equals("N"))) {
				mol.setAtomFlag(i, Molecule3D.BACKBONE, true);
			}
		}

		// Load stable water
		//if (/*isLoadStableWater &&*/ isLoadWater) {
			List<int[]> interactions = getInterMolecularInteractions(mol);
			int[] cc = new int[mol.getAllAtoms()];
			for (int[] pair: interactions) {
				if (mol.getAtomicNo(pair[0]) == 8
						&& mol.getConnAtoms(pair[0]) == 0
						&& (mol.getAtomicNo(pair[1]) == 7 || mol.getAtomicNo(pair[1]) == 8)
						&& mol.getConnAtoms(pair[1]) > 0)
					cc[pair[0]]++;
				if (mol.getAtomicNo(pair[1]) == 8
						&& mol.getConnAtoms(pair[1]) == 0
						&& (mol.getAtomicNo(pair[0]) == 7 || mol.getAtomicNo(pair[0]) == 8)
						&& mol.getConnAtoms(pair[0]) > 0)
					cc[pair[1]]++;
			}
			List<Integer> keep = new ArrayList<Integer>();
			for (int i = 0; i < cc.length; i++) {
				if (cc[i] >= 2) {
					keep.add(i);
				}
			}
			for (int i = mol.getAllAtoms() - 1; i >= 0; i--) {
				if (mol.getAtomicNo(i) == 8 && mol.getAllConnAtoms(i) == 0 ) {
					if(keep.contains(i)) {					
						mol.setAtomFlag(i, Molecule3D.IMPORTANT, true);
					} else {
						mol.setAtomFlag(i, Molecule3D.IMPORTANT, false);
						if(!isLoadWater()) mol.deleteAtom(i);
					}
				}
			}
		//}

		mol.compact();
		for (int i = 0; i < mol.getAllAtoms(); i++) {
			
			if(mol.getConnAtoms(i)==0) {
				mol.setAtomFlag(i, Molecule3D.LIGAND, false);
			}
			
			mol.setAtomFlag(i, Molecule3D.RIGID, !mol.isAtomFlag(i, Molecule3D.LIGAND));
			mol.setAtomFlag(i, Molecule3D.PREOPTIMIZED, true);
		}
		
		
		res.add(mol);
	}
	
	@Override
	public List<Molecule3D> loadGroup(String fileName, Reader in, int from, int to) throws Exception {
		
		String line;
		resolution = -1;
		int structure = 0;
		LineNumberReader reader = null;
		String dubiousOcc = null;
		
		reader = new LineNumberReader(in);
		Map<String, Integer> allAtomIds = new HashMap<String, Integer>();
		Map<Integer, String> atomToGroup = new HashMap<Integer, String>();
		List<Integer> markedLigs = new ArrayList<Integer>();
		proteinName = "";

		int model = 0;

		Molecule3D mol = new Molecule3D();
		List<Molecule3D> res = new ArrayList<Molecule3D>();

		pqr = false;
		while ((line = reader.readLine()) != null) {
			
			if (line.length() < 7) continue;
			String type = line.substring(0, 7);
			boolean isTypeAtom = type.startsWith("ATOM ");
			boolean isTypeHet = type.startsWith("HETATM");
			if (type.startsWith("MODEL ")) {
				String n = new StringTokenizer(line.substring(6)).nextToken().trim();
				structure++;
				try {
					addMol(res, mol, fileName, model, atomToGroup, markedLigs);
					model = Integer.parseInt(n);					
					mol = new Molecule3D();
					markedLigs.clear();
				} catch (Exception e) {
					System.err.println(e);
				}
			} 
			if(loadModel>=0) {
				if(model!=loadModel) continue;
			}
			
			
			if (type.startsWith("TER ")) {
				structure++;
			} else if (isTypeAtom || (isLoadHeteroAtoms && isTypeHet)) {				
				//New atom to add
				if (line.length() <= 60) {
					if (isLenient) continue;
					throw new IOException("invalid line: " + line);
				}

				String description = line.substring(7, 30).trim();
				String id = line.substring(7, 12).trim();
				String elt2Prefix = line.substring(12, 13).trim().toUpperCase(); // A or N or ''
				String atomName = line.substring(13, 17).trim().toUpperCase(); // Element
				String amino = line.substring(17, 20).trim();
				String chainId = line.substring(21, 22).trim();
				String sequence = line.substring(22, 30).trim(); // Name
				String elt1 = line.length() > 79 ? line.substring(75, 80).trim() : ""; // Element
				String xPosition = line.substring(30, 38).trim(); // X
				String yPosition = line.substring(38, 46).trim(); // Y
				String zPosition = line.substring(46, 54).trim(); // Z
				String occupancy = line.substring(54, 62).trim();
				String alt2 = line.substring(16, 17).trim();
				String elt;

				if(loadChain!=null && !loadChain.equals(chainId) && !isTypeHet && !amino.equals("ACT")) continue;
				
				if(atomName.length()==0) continue;
				if (!isLoadHalfOccupancy && alt2.equals("B")) continue; // Alternate position
				if (!isLoadHalfOccupancy && alt2.equals("2")) continue; // Alternate position
				if (!isLoadHalfOccupancy && alt2.equals("3")) continue; // Alternate position
				
				if (elt1.length() > 0 && elt1.length() < 3) {
					elt = elt1;
				} else {
					if (Character.isDigit(atomName.charAt(0))) {
						elt = atomName.substring(1);
					} else {
						elt = atomName;
					}
					if (elt2Prefix.length() > 0
							&& !Character.isDigit(elt2Prefix.charAt(0))
							&& elt2Prefix.charAt(0) != 'N'
							&& elt2Prefix.charAt(0) != 'A') {
						elt = elt2Prefix + elt;
						elt2Prefix = "";
					}
				}
				
				if (elt.equals("D") || elt.startsWith("Q") || elt.startsWith("DUM")) continue; // Dummy atom
			
				if (/*!isLoadStableWater &&*/ !isLoadWater && amino.equalsIgnoreCase("HOH")) continue;

				if(!pqr) {
					double occ = 1;
				
					try { occ = Double.parseDouble(occupancy);} catch (Exception e) {}
					if (occ>0) {
//						if (sequence.equals(previousResidue)) count++;
//						previousResidue = sequence;	
		
						// Treat occupancy <=.5
						if(!isLoadHalfOccupancy) {
							if((amino.endsWith("B") || amino.startsWith("2")  || amino.startsWith("3")) && occ<=.5) continue;
							if (occ == 0.5) {							
								if (description.equalsIgnoreCase(dubiousOcc)) continue;
								dubiousOcc = description;
							} else {
								if (occ <= 0.3) continue;
								dubiousOcc = null;
							}
						}
					}	
				}
				 
//				read++;

				int atomNo = 0;
				if (elt.startsWith("'")) elt = elt.substring(1);
				else if (elt.startsWith("R")) elt = elt.substring(1);
				else if (elt.startsWith("X")) elt = elt.substring(1);
				else if (elt.startsWith("*")) elt = elt.substring(1);
				
				elt = wipeDigits(elt);
				
				if (elt.equals("AD1")) atomNo = 8;
				else if (elt.equals("AE1")) atomNo = 8;
				else if (elt.equals("AD2")) atomNo = 7;
				else if (elt.equals("AE2")) atomNo = 7;
				else if (elt.startsWith("H")) atomNo = 1;
				else if (elt.startsWith("FE")) atomNo = Molecule.getAtomicNoFromLabel("Fe");
				else if (elt.startsWith("CL")) atomNo = Molecule.getAtomicNoFromLabel("Cl");
				else if (elt.startsWith("BR")) atomNo = Molecule.getAtomicNoFromLabel("Br");
				else if (elt.startsWith("HG")) atomNo = Molecule.getAtomicNoFromLabel("Hg");
				else if (elt.startsWith("SI")) atomNo = Molecule.getAtomicNoFromLabel("Si");
				else if(elt.length()>0) atomNo = Molecule.getAtomicNoFromLabel(wipeDigits(elt.substring(0, 1)));
				
				if ((atomNo >= 100 || atomNo == 0) && elt.length() >= 2) atomNo = Molecule.getAtomicNoFromLabel(wipeDigits(elt.substring(0, 2)));
				if ((atomNo >= 100 || atomNo == 0) && elt.length() >= 2) atomNo = Molecule.getAtomicNoFromLabel(wipeDigits(elt.substring(1)));
				if ((atomNo >= 100 || atomNo == 0) && (elt2Prefix + elt).length() >= 2) atomNo = Molecule.getAtomicNoFromLabel((elt2Prefix + elt) .substring(0, 2));

				if (atomNo >= 100 || atomNo == 0) {
					if (isLenient) {System.err.println("Unknown atom " + elt);continue;}
					throw new IOException("The file has an element '" + elt + "' with an atomic number of " + atomNo + " at line " + reader.getLineNumber());
				}
				if (atomNo >= 2 || (isLoadHydrogen && atomNo >= 1)) {
					double x = Double.parseDouble(xPosition);
					double y = Double.parseDouble(yPosition);
					double z = Double.parseDouble(zPosition);

					if (x >= 9999) {
						System.err.println("Invalid coordinates for " + elt);
						continue; // Dummy atom
					}

					String name = "";
					for (int i = 0; i < atomName.length() && name.length() < 2; i++) {
						if (Character.isLetter(atomName.charAt(i))) name += atomName.charAt(i);
					}
					if (name == "") name = Molecule.cAtomLabel[mol.getAtomicNo(atomNo)];

					// Add the atom		
					int atom = mol.addAtom(atomNo);
					mol.setAtomX(atom, x);
					mol.setAtomY(atom, y);
					mol.setAtomZ(atom, z);
					mol.setAtomDescription(atom, description);
					mol.setAtomName(atom, atomName);
					mol.setAtomAmino(atom, amino);
					if(pqr) {
						try {
							mol.setPartialCharge(atom, Double.parseDouble(occupancy));
						} catch (Exception e) {
						}
					}
					try {
						mol.setAtomSequence(atom, Integer.parseInt(sequence));
					} catch (Exception e) {
					}
					mol.setAtomChainId(atom, chainId);
					
					if ("LIG".equals(amino) || "ACT".equals(amino)) markedLigs.add(atom);
					
					allAtomIds.put(id, atom);
					
					String groupId  = isTypeAtom? (""+structure) : (sequence);
					atomToGroup.put(atom, groupId);
					if(isTypeHet)  mol.setAtomFlag(atom, Molecule3D.LIGAND, true);
				}
			} else if(type.startsWith("BOND ") ) { // Bond information (ACT specific)
				String id1 = line.substring(7, 13).trim();
				String id2 = line.substring(13, 18).trim();
				String order = line.substring(18, 23).trim();
				Integer atom1 = allAtomIds.get(id1);
				Integer atom2 = allAtomIds.get(id2);
				mol.addBond(atom1, atom2, Integer.parseInt(order));
				
			} else if (line.startsWith("CRYST1")) {
				crystal = line.substring(7);
				if (crystal.length() > 63)
					crystal = crystal.substring(0, 63);
				crystal = crystal.trim();
			} else if (line.startsWith("ORIGX")) {

			} else if (line.startsWith("REMARK")) {
				if (line.indexOf(" PQR ") > 0) {
					pqr = true;
				}
				if (line.indexOf(" RESOLUTION. ") > 0) {
					try {
						String s = line
								.substring(line.indexOf(" RESOLUTION. ") + 2);
						StringTokenizer st = new StringTokenizer(s);
						st.nextToken();
						if (st.hasMoreTokens()) {
							String resString = st.nextToken();
							resolution = Double.parseDouble(resString);
							if(resolution>100) resolution/=100;
						}
					} catch (Exception e) {
						// Nothing
					}
				} else if (line.indexOf(" RESOLUTION RANGE HIGH ") > 0
						&& resolution < 0) {
					try {
						int end = line.indexOf("      ");
						if (end > 0)
							line = line.substring(0, end);
						StringTokenizer st = new StringTokenizer(line);
						String last = "";
						while (st.hasMoreTokens())
							last = st.nextToken();
						resolution = Double.parseDouble(last);
					} catch (Exception e) {
						// Nothing
					}
				}
			} else if (line.startsWith("TITLE")) {				
				proteinName += (proteinName.length()>0?" ":"")+line.substring(10).trim();
			} else if (line.startsWith("HEADER")) {
				try {
					classification = line.substring(10,50).trim();
					depositionDate = new SimpleDateFormat("dd-MMM-yy").parse(line.substring(50,60).trim());
				} catch (Exception e) {					
				}
			}
		}
		
		addMol(res, mol, fileName, model, atomToGroup, markedLigs);
		
		in.close();

		return res;

	}

	/**
	 * @return
	 */
	public boolean isCreateConnections() {
		return isCreateConnections;
	}

	/**
	 * @return
	 */
	public boolean isLoadHeteroAtoms() {
		return isLoadHeteroAtoms;
	}

	/**
	 * @param b
	 */
	public void setCreateConnections(boolean b) {
		isCreateConnections = b;
	}

	/**
	 * @param b
	 */
	public void setLoadHeteroAtoms(boolean b) {
		isLoadHeteroAtoms = b;
	}

	/**
	 * @return
	 */
	public boolean isLoadHydrogen() {
		return isLoadHydrogen;
	}

	/**
	 * @param b
	 */
	public void setLoadHydrogen(boolean b) {
		isLoadHydrogen = b;
	}

	/**
	 * @return
	 */
	public boolean isLoadHalfOccupancy() {
		return isLoadHalfOccupancy;
	}

	/**
	 * @param b
	 */
	public void setLoadHalfOccupancy(boolean b) {
		isLoadHalfOccupancy = b;
	}

	/**
	 * @return
	 */
	public boolean isCreateBondOrders() {
		return isCreateBondOrders;
	}

	/**
	 * @param b
	 */
	public void setCreateBondOrders(boolean b) {
		isCreateBondOrders = b;
	}

	/**
	 * @return
	 */
	public double getResolution() {
		return resolution;
	}

	/**
	 * @return
	 */
	public boolean isLoadWater() {
		return isLoadWater;
	}

	/**
	 * @param b
	 */
	public void setLoadWater(boolean b) {
		isLoadWater = b;
	}

	/**
	 * @return
	 */
	public boolean isLenient() {
		return isLenient;
	}

	/**
	 * @param b
	 */
	public void setLenient(boolean b) {
		isLenient = b;
	}

	/**
	 * @return
	 */
	public String getProteinName() {
		return proteinName;
	}

	/**
	 * @return
	 */
	public boolean isLoadSalts() {
		return isLoadSalts;
	}

	/**
	 * @param b
	 */
	public void setLoadSalts(boolean b) {
		isLoadSalts = b;
	}

	/**
	 * @return
	 */
	public String getCrystal() {
		return crystal;
	}

	public static final ParserFileFilter FILEFILTER = new ParserFileFilter() {
		@Override
		public boolean accept(File f) {
			return f.getName().toUpperCase().endsWith(".PDB")
					|| f.getName().toUpperCase().endsWith(".ENT")
					|| f.getName().toUpperCase().endsWith(".PQR")
					|| f.isDirectory();
		}
		@Override
		public String getDescription() {
			return "PDB File";
		}
		@Override
		public String getExtension() {
			return ".pdb";
		}
	};

	/**
	 * @return
	 */
	public boolean isLoadMainStructureOnly() {
		return isKeepMainStructureOnly;
	}

	/**
	 * @param b
	 */
	public void setLoadMainStructureOnly(boolean b) {
		isKeepMainStructureOnly = b;
	}

	@Override
	public void save(Molecule3D mol, Writer writer) throws Exception {
		save(Collections.singletonList(mol), writer);
	}
	
	@Override
	public void save(List<Molecule3D> mols, Writer writer) throws Exception {
		DecimalFormat df = new DecimalFormat("0.000");
		
		// Write the atoms
		writer.write("HEADER    " + NEWLINE);
		writer.write("REMARK   1 AUTH WRITTEN BY ACT3D" + NEWLINE);
		int model = 0;
		for (Molecule3D mol : mols) {
			if(mols.size()>0) {
				writer.write("MODEL " + (model++));
				writer.write(NEWLINE);
			}
			int count = 0;
			for (int t = 0; t < 2; t++) {
				for (int i = 0; i < mol.getAllAtoms(); i++) {
	
					if (mol.getAtomicNo(i) <= 0) continue; // Lp
					if (t == 0 && mol.isAtomFlag(i, Molecule3D.LIGAND)) continue;
					if (t == 1 && !mol.isAtomFlag(i, Molecule3D.LIGAND)) continue;
						
					String name = mol.getAtomName(i);
					if(name==null) name = Molecule.cAtomLabel[mol.getAtomicNo(i)];
					
					String chain = mol.getAtomChainId(i);
					if(chain==null) chain = t == 0 ? "A" : "L";
					
					String amino = mol.getAtomAmino(i);
					if(amino==null) amino = t == 0 ? "PRO" : "LIG";
	
					int seq = mol.getAtomSequence(i);
	
					if (t == 0) writeL(writer, "ATOM  ", 6);
					else if (t == 1) writeL(writer, "HETATM", 6);
					
					writeR(writer, "" + (++count), 5);
					writeR(writer, "  ", 2);
					writeL(writer, name, 4);
					writeR(writer, amino, 3);
					writeR(writer, chain, 2);
					writeR(writer, "" + seq, 4);
				
					writeR(writer, df.format(mol.getAtomX(i)), 12);
					writeR(writer, df.format(mol.getAtomY(i)), 8);
					writeR(writer, df.format(mol.getAtomZ(i)), 8);
					writer.write("  1.00  0.00");
					writeR(writer, ExtendedMolecule.cAtomLabel[mol.getAtomicNo(i)],
							12);
					writer.write(NEWLINE);
				}
			}
		}
	}

	
	private static String wipeDigits(String s) {
		String res = "";
		for (int i = 0; i < s.length(); i++) {
			if(Character.isLetter(s.charAt(i))) res+=s.charAt(i);
		}
		return res;
	}

	public Date getDepositionDate() {
		return depositionDate;
	}
	
	public String getClassification() {
		return classification;
	}

	public int getLoadModel() {
		return loadModel;
	}

	public void setLoadModel(int loadModel) {
		this.loadModel = loadModel;
	}

	public String getLoadChain() {
		return loadChain;
	}

	public void setLoadChain(String loadChain) {
		this.loadChain = loadChain;
	}

	public void flagBackbone(Molecule3D mol) {
		boolean[] backbone = getBackbones(mol);
		for (int i = 0; i < backbone.length; i++) {
			mol.setAtomFlag(i, Molecule3D.BACKBONE, backbone[i]);
		}
	}

	public static boolean[] getBackbones(Molecule3D molecule) {
		return getBackbones(molecule, 70);
	}

	/**
	 * Gets the backbone
	 * @param molecule
	 * @return an array of boolean
	 */
	public static boolean[] getBackbones(Molecule3D molecule, int minChain) {
		boolean[] inBackbone = new boolean[molecule.getAllAtoms()];

		//Find the fragments (connex components of the graph)
		List<List<Integer>> fragments = getConnexComponents(molecule);


		//For each fragment, find the longest chain
		//List res[] = new List[fragments.size()];
		for(int i=0; i<fragments.size(); i++) {
			boolean markAll = false;

			//Find an extremity
			List<Integer> fragment = fragments.get(i);
			List<Integer> l = null;
			int root = fragment.get(0);

			if(markAll) {
				for(int j=0; j<fragment.size(); j++) {
					inBackbone[((Integer)fragment.get(j)).intValue()] = true;
				}
			} else {
				l = getLongestChain(molecule, root);
				root = ((Integer)l.get(l.size()-1)).intValue();
				l = getLongestChain(molecule, root);
				if(l.size()<minChain && molecule.getAllAtoms()>80) markAll=true;
				for(int j=0; j<l.size(); j++) {
					inBackbone[((Integer)l.get(j)).intValue()] = true;
				}
			}
		}

		return inBackbone;
	}

	/**
	 * Return a List of int[] representing all the atom-atom pairs having
	 * an intermolecular interactions (distance close to sum of VDW)
	 * @param mol
	 * @return
	 */
	public static List<int[]> getInterMolecularInteractions(Molecule3D mol) {
		//long s = System.currentTimeMillis();
		int[] atomToGroups = getAtomToGroups(mol);
		List<int[]> res = new ArrayList<int[]>();
		MoleculeGrid grid = new MoleculeGrid(mol);
		for(int a1=0; a1<mol.getAllAtoms(); a1++) {
			if(mol.getAtomicNo(a1)<=1) continue;
			Set<Integer> neighbours = grid.getNeighbours(new Coordinates(mol.getAtomX(a1),mol.getAtomY(a1),mol.getAtomZ(a1)), 4);
			Iterator<Integer> iter = neighbours.iterator();
			while(iter.hasNext()) {
				int a2 = ((Integer)iter.next()).intValue();
				if(a2<a1) continue;
				if(mol.getAtomicNo(a2)<=1) continue;
				if(mol.getAllConnAtoms(a1)>0 && mol.getAllConnAtoms(a2)>0 && atomToGroups[a1]==atomToGroups[a2]) continue;
				double r2 = mol.getCoordinates(a1).distanceSquared(mol.getCoordinates(a2));
				double vdw = VDWRadii.VDW_RADIUS[mol.getAtomicNo(a1)] + VDWRadii.VDW_RADIUS[mol.getAtomicNo(a2)];

				if( r2>(vdw-.5)*(vdw-.5) && r2 < vdw*vdw) {
					res.add(new int[]{a1, a2});
				}
			}
		}
		//System.out.println("inter: "+(System.currentTimeMillis()-s)+"ms");
		return res;
	}

	/**
	 * Returns a List of all Connex Components of the graph (List of List of Integer)
	 * <pre>
	 * Example:
	 *
	 *  The following molecule:
	 *
	 * 			  -- 3
	 * 	   1 -- 2                 5 -- 6              7
	 * 	          -- 4
	 * will return
	 *  [[1,2,3,4],[5,6],[7]]
	 * </pre>
	 * Complexity: O(nAtoms)
	 * Memory: O(nAtoms)
	 */
	public static List<List<Integer>> getConnexComponents(Molecule3D mol) {
		int[] groups = getAtomToGroups(mol);
		int nGroups = 0;
		for(int i=0; i<groups.length; i++) {
			if(groups[i]>nGroups) nGroups = groups[i];
		}
		List<List<Integer>> r = new ArrayList<List<Integer>>();
		for(int i=0; i<nGroups; i++) r.add(new ArrayList<Integer>());
		for(int i=0; i<groups.length; i++) {
			r.get(groups[i]-1).add(i);
		}
		return r;
	}

	public static int[] getAtomToGroups(Molecule3D mol) {
		return getAtomToGroups(mol, null);
	}
	/**
	 * For each molecule in <code>mol</code> one group is created.
	 * The group index starts with 1. The size of the array returned is equal to the number of atoms.
	 *
	 * @param mol
	 * @param seeds
	 * @return a int[] array such as array[atom] = groupNo >=1
	 */
	public static int[] getAtomToGroups(Molecule3D mol, List<Integer> seeds) {
		int[] res = new int[mol.getAllAtoms()];
		IntQueue q =  new IntQueue();
		int nGroups = 0;
		for(int i=0; i<res.length; i++) {
			if(res[i]>0) continue;
			if(seeds!=null) seeds.add(i);
			q.push(i);
			nGroups++;
			while(!q.isEmpty()) {
				int a = q.pop();
				res[a] = nGroups;
				for(int j=0; j<mol.getAllConnAtoms(a); j++) {
					int a2 = mol.getConnAtom(a, j);
					if(res[a2]==0) {
						q.push(a2);
						res[a2]=-1; //safeguard
					}
				}
			}
			q.clear();
		}
		return res;
	}

	private static class Item {
		public Item(int a) {this.a = a;path.add(a);}
		public int a;
		public ArrayList<Integer> path = new ArrayList<Integer> ();
	}

	/**
	 * Return the longest molecular chain starting at atm
	 * @param mol
	 * @param atm
	 * @return a List of Integer
	 */
	@SuppressWarnings("unchecked")
	public static List<Integer> getLongestChain(Molecule3D mol, int atm) {
		LinkedList<Item> q = new LinkedList<Item>();
		q.add(new Item(atm));
		boolean[] seen = new boolean[mol.getAllBonds()*2];
		try {
			while(true) {
				Item item = q.removeFirst();
				int a = item.a;
				for(int i=0; i<mol.getAllConnAtoms(a); i++) {
					int b = mol.getConnBond(a, i);
					int a2 = mol.getConnAtom(a, i);
					int bi = b*2 + (a2>a?1:0);
					if(seen[bi]) continue;
					seen[bi] = true;
					if(mol.getAtomicNo(a2)<=1) continue;
					Item ni = new Item(a2);
					ni.a = a2;
					ni.path = (ArrayList<Integer>) item.path.clone();
					ni.path.add(a2);
					q.add(ni);
				}
				if(q.isEmpty()) return item.path;
			}
		}catch(Exception e ){
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Extract the main structure
	 * @param mol
	 * @return
	 */
	public static void extractMainStructure(Molecule3D mol, Molecule3D res) {
		List<List<Integer>> cc = getConnexComponents(mol);
		int[] sizes = new int[cc.size()];
		for (int i = 0; i < sizes.length; i++) sizes[i] = cc.get(i).size();
		
		
		//Find the main structure: take the biggest one
		int biggest = 0;
		for (int i = 0; i < sizes.length; i++) {
			if(sizes[biggest]<sizes[i]) biggest = i;
		}		
		List<Integer> main = cc.get(biggest);
		Coordinates mainMin = mol.getCoordinates(main.get(0));
		Coordinates mainMax = mol.getCoordinates(main.get(0));
		for (int j = 1; j < main.size(); j++) {
			int a = main.get(j);
			mainMin = mainMin.min(mol.getCoordinates(a));
			mainMax = mainMax.max(mol.getCoordinates(a));
		}
		
		//Find the interconnected structures
		List<Integer> toBeAdded = new ArrayList<Integer>();
		MoleculeGrid grid = new MoleculeGrid(mol);
		for (int i = 0; i < cc.size(); i++) {
			if(i==biggest) continue;
			if(sizes[i]>sizes[biggest]*.98) continue;
			List<Integer> g = cc.get(i);
			Coordinates min = mol.getCoordinates(g.get(0));
			Coordinates max = mol.getCoordinates(g.get(0));
			for (int j = 1; j < g.size(); j++) {
				int a = g.get(j);
				min = min.min(mol.getCoordinates(a));
				max = max.max(mol.getCoordinates(a));
			}
			
			if(mainMax.x<min.x || mainMin.x>max.x) continue;
			if(mainMax.y<min.y || mainMin.y>max.y) continue;
			if(mainMax.z<min.z || mainMin.z>max.z) continue;
			
			Coordinates interMin = min.max(mainMin);
			Coordinates interMax = max.min(mainMax);			
						
			double dist = interMin.distance(interMax);
			double dist2 = min.distance(max);
			if(dist>2*dist2/3) {
				Set<Integer> neigh = grid.getNeighbours(new Coordinates[]{min, max}, 4);
				neigh.retainAll(main);
				if(neigh.size()>00) {
					toBeAdded.addAll(g);
				}
			} 
		}
		
		res.clear();				
		main.addAll(toBeAdded);		
		copyAtoms(mol, res, main);
	}

	public static void copyAtoms(Molecule3D mol, Molecule3D res, List<Integer> atomsToBeAdded) {

		Map <Integer, Integer> molToRes = new HashMap<Integer, Integer>();
		//Copy the atoms
		for(int i=0; i<atomsToBeAdded.size(); i++) {
			int a = atomsToBeAdded.get(i);
			if(molToRes.containsKey(a)) continue;
			int n = res.addAtom(mol, a);
			molToRes.put(a,n);
		}

		//Add the bonds
		for(int i=0; i<mol.getAllBonds(); i++) {
			int mol1 = mol.getBondAtom(0, i);
			int mol2 = mol.getBondAtom(1, i);

			Integer a1 = molToRes.get(mol1);
			Integer a2 = molToRes.get(mol2);
			if(a1!=null && a2!=null) {
				res.addBond(a1.intValue(), a2.intValue(), mol.getBondOrder(i));
			}
		}
	}

	public static void main(String[] args) throws Exception {
		List<Molecule3D> res = new PDBFileParser().loadGroup("D:/dev/Java/Actelion3D/DUDtest/kinHSP90/1uy6.pdb");
		new PDBFileParser().save(res.get(0), "c:/1uy6.pdb");
		new PDBFileParser().loadGroup("c:/1uy6.pdb");
		System.out.println("models=" +res.size());
	}
}
