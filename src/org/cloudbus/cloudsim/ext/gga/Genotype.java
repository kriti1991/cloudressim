package org.cloudbus.cloudsim.ext.gga;

import org.cloudbus.cloudsim.ext.gga.enums.PackingT;
import java.util.Random;
import java.util.Stack;

public class Genotype {
	static private int idnum = 0;
	static private Random rnd = new Random(554881166);
	static private Problem problem;

	private int idTag; // unique number
	private double fitness; // current fitness
	private int nrOfObjects; // number of objects
	private int nrOfGroups; // number of groups
	private int nrOfPacks; // number of colors in problem
	private double allEleMutationProb; // chance group is deleted in mutation
	private PackingT packingUsed; // what algo to use for coloring nodes
	private int objects[]; // holds objects
	private int groups[]; // holds groups

	public Genotype() {
		this.objects = new int[Constants.MAXOBJECTS];
		this.objects = new int[Constants.MAXOBJECTS];
		this.packingUsed = PackingT.FISTFIT;
	}

	public void Initialize(int numberOfObjects,
			double allElemutationProbability, int kBinpacking,
			PackingT packingAlgorithm)
	// Initialize the geno by making a coloring using the
	// PackObject function, and starting at a random node.
	{
		int i, r;

		r = rnd.nextInt();

		idTag = idnum;
		idnum++;
		allEleMutationProb = allElemutationProbability;

		packingUsed = packingAlgorithm;

		if (packingUsed == PackingT.UNDIFINED) {
			System.err.println("Error: No coloring algorithm defined");
			System.exit(2);
		}

		nrOfGroups = 0;
		nrOfPacks = kBinpacking;
		nrOfObjects = numberOfObjects;

		if (nrOfObjects > Constants.MAXOBJECTS) {
			System.err.println("Error: number of objects is larger than "
					+ Constants.MAXOBJECTS);
			System.exit(2);
		}

		for (i = 0; i < nrOfObjects; i++)
			objects[i] = Constants.UNCOLORED;
		for (i = 0; i < nrOfObjects; i++)
			PackObject((i + r) % nrOfObjects);

		Evaluate();

	}

	public void Evaluate() {

	}

	public void Mutation()
	// Mutate a geno by eliminating some groups and reinserting
	// the objects using the PackObject function.
	{
		int i;
		Stack<Integer> savedObjects = new Stack<Integer>();			// objects form eliminated groups
		boolean eliminated [] = new boolean[Constants.MAXOBJECTS];	// marker-array for eliminated groups

		//cerr << "mutation " << idtag << endl;

		for (i = 0; i < nrOfGroups; i++)
				eliminated[i] = false;

		// pick colors for elimination
		for (i = 0; i < nrOfGroups; i++)
			if ((rnd.nextInt () % 100) <= (allEleMutationProb * 100))
			{
				eliminated[groups[i]] = true;
				groups[i] = Constants.ELIMINATED;
			}

		// Pick all colors that were in an eliminated group and mark
		// them uncolored
		for (i = 0; i < nrOfObjects; i++)
		{
			if (eliminated[objects[i]])
			{
				objects[i] = Constants.UNCOLORED;
				savedObjects.push (i);
			}
		}

		// Remove holes in group part created by elimination
		CompactGroupPart ();

		// Reinsert uncolored objects with PackObject function
		while (!savedObjects.empty ())
			PackObject (savedObjects.pop ());

		// Reevaluate geno
		Evaluate ();


	} // Mutation ()

	public void Crossover(Genotype otherParent, Genotype child1, Genotype child2)
	// Do a crossover operation between the geno and another parent,
	// producing two children. Using the procedure described by
	// E.Falkenhauer and the PackObject function to reinsert objects.
	{
		int i, j;
		int p1cp1, p1cp2;				// crossover-points for parent P1
		int p2cp1, p2cp2;				// crossover-points for parent P2
		boolean eliminated1[] = new boolean[Constants.MAXOBJECTS];	// marker-array for elimination process P1
		boolean eliminated2[] = new boolean[Constants.MAXOBJECTS];	// marker-array for elimination process P2
		Stack<Integer> objects1 = new Stack<Integer>();				// holds objects from eliminated groups P1
		Stack<Integer> objects2 = new Stack<Integer>();				// holds objects from eliminated groups P2

		for (i = 0; i < nrOfGroups; i++)
		{
			eliminated1[i] = false;
			eliminated2[i] = false;
		}

		//cerr << "crossover " << idtag << " " << otherParent.idtag << " " << child1.idtag << " " << child2.idtag << endl;

		// Choose crossover points
		p1cp1 = rnd.nextInt () % nrOfGroups;
		p1cp2 = rnd.nextInt () % nrOfGroups;
		if (p1cp2 < p1cp1)
		{
			i = p1cp1;
			p1cp1 = p1cp2;
			p1cp2 = i;
		}
		p2cp1 = rnd.nextInt () % otherParent.nrOfGroups;
		p2cp2 = rnd.nextInt () % otherParent.nrOfGroups;
		if (p2cp2 < p2cp1)
		{
			i = p2cp1;
			p2cp1 = p2cp2;
			p2cp2 = i;
		}

		// Copy parents to children
		Copy (child1);
		otherParent.Copy (child2);

		// Mark all groups losing at least one object with ELIMINATED
		for (i = 0; i < nrOfObjects; i++)			// look at all objects
			for (j = p1cp1; j <= p1cp2; j++)		// walk through crossing-section
				if (objects[i] == groups[j])		// object is in injected group
				{
					eliminated2[child2.objects[i]] = true;		// mark group affected
					child2.objects[i] = groups[j] + child2.nrOfGroups;	// new color
				}
		for (i = 0; i < otherParent.nrOfObjects; i++)
			for (j = p2cp1; j <= p2cp2; j++)
				if (otherParent.objects[i] == otherParent.groups[j])
				{
					eliminated1[child1.objects[i]] = true;
					child1.objects[i] = otherParent.groups[j] + child1.nrOfGroups;
				}

		// Eliminate effected groups
		for (i = 0; i < child1.nrOfGroups; i++)
			if (eliminated1[child1.groups[i]])
					child1.groups[i] = Constants.ELIMINATED;

		for (i = 0; i < child2.nrOfGroups; i++)
			if (eliminated2[child2.groups[i]])
					child2.groups[i] = Constants.ELIMINATED;

		// Collect objects member of an eliminated group
		for (i = 0; i < child2.nrOfObjects; i++)
			if ((child2.objects[i] < child2.nrOfGroups) && (eliminated2[child2.objects[i]]))
			{
				child2.objects[i] = Constants.UNCOLORED;
				objects2.push (i);
			}
		for (i = 0; i < child1.nrOfObjects; i++)
			if ((child1.objects[i] < child1.nrOfGroups) && (eliminated1[child1.objects[i]]))
			{
				child1.objects[i] = Constants.UNCOLORED;
				objects1.push (i);
			}

		// Inject group-part from parents into children
		child2.InsertGroup (groups, p1cp1, p1cp2, p2cp1);
		child1.InsertGroup (otherParent.groups, p2cp1, p2cp2, p1cp1);

		// Remove holes in group-array created by the elimination process
		child2.CompactGroupPart ();
		child1.CompactGroupPart ();

		// Reinsert objects from eliminted groups
		while (!objects2.empty ())
			child2.PackObject (objects2.pop ());
		while (!objects1.empty ())
			child1.PackObject (objects1.pop ());

		// Compute fitness of children
		child2.Evaluate ();
		child1.Evaluate ();

	} // Crossover ()
		
	public void Print()
	// Print out the geno's genes and it's fitness.
	{
		int i;

		System.out.println("(" + idTag + ") ");

		// Print out objects
		for (i = 0; i < nrOfObjects; i++)
			if (objects[i] == Constants.UNCOLORED)
				System.out.println("X ");
			else
				System.out.println(objects[i]+ " ");

		System.out.println(" : ");

		// Print out groups
		for (i = 0; i < nrOfGroups; i++)
			if (groups[i] == Constants.ELIMINATED)
				System.out.println("X ");
			else
				System.out.println(groups[i]+" ");

		System.out.println(", ");

		// Print out fitness
		System.out.println("fitness: "+fitness);
	}

	public int GetBinsUsed() {
		return nrOfGroups;
	}

	public double GetFitness() {
		return fitness;
	}

	public void Copy(Genotype child) 
	// Copy the geno to the supplied recipiant.
	{
		int i;

		for (i = 0; i < nrOfObjects; i++)
			child.objects[i] = objects[i];
		for (i = 0; i < nrOfGroups; i++)
			child.groups[i] = groups[i];

		child.nrOfObjects = nrOfObjects;
		child.nrOfGroups = nrOfGroups;
		child.fitness = fitness;

	}

	public int IsValid(int object)
	// Check if all objects in the geno do not violate a solution, and
	// check if there are no duplicate colors.
	{
		return 1;
	}

	private int ViolatedConstraints (int object)
	// Calculate the number of constraints an object violates.
	{
		int violations = 0;
		boolean success = false;
		
		int group = objects[object];
		
		Capacity size = problem.GetBinSize();
		
		for (int i=0; i < nrOfObjects; i++) {
			if (objects[i] == group) {
				//����i��item�ŵ�bin��ͷ��size��¼��ǰ���binʣ������
				success = problem.PutItem(size, i);
				//������ܷ���Ļ�������>0��violations����ʾ�����ֹ����������Ҫ��
				if (!success) return 1;
			}
		}
			
		return violations;
	} // ViolatedConstraints ()

	private void PackObject(int object)
	// Pack an object using the algoritm selected.
	{
		switch (packingUsed) {
		case FISTFIT:
			PackObject_FirstFit(object);
			break;
		// These two have to be implemented
		/*
		 * case smallfirst: PackObject_OrderedPacking (object); break; case
		 * largefirst: PackObject_OrderedPacking (object); break;
		 */
		default:
			System.err.println("Error: No coloring algorithm defined");
			System.exit(2);
		}

	} // PackObject ()

	private void PackObject_FirstFit(int object)
	// Packs an object by using a first fit heurist, if no color
	// is available it creates a new group and uses this to color
	// the object with.
	{
		int i = 0;

		// First Fit Packing
		objects[object] = 0;
		while (ViolatedConstraints(object) > 0) {
			i++;
			objects[object] = i;
		}

		// New color used, update number of groups and
		// add a new color to the group-part
		if (i + 1 > nrOfGroups) {
			nrOfGroups = i + 1;
			groups[nrOfGroups - 1] = nrOfGroups - 1;
		}

	} // PackObject_FirstFit ()

	private void InsertGroup(int parentGroups[], int cp1, int cp2, int position)
	// Given the group-part and the crossing-points of another geno,
	// this method inserts the groups between these points on the
	// given position.
	{
		int i;

		// Make room for to-be-inserted-groups
		for (i = nrOfGroups; i > position; i--)
			groups[i + (cp2 - cp1)] = groups[i - 1];

		// Inject groups in the gene
		for (i = cp1; i <= cp2; i++)
			groups[i + position - cp1] = parentGroups[i] + nrOfGroups;

		// Update number of groups
		nrOfGroups = nrOfGroups + (cp2 - cp1) + 1;

	} // InsertGroup ()

	private void CompactGroupPart()
	// After elimination of groups from the group-part of the gene,
	// this method removes the holes created by elimination and
	// renumbers the remaining groups to create a numbering between
	// 0 and nrOfGroups - 1.
	{
		int i, j;
		boolean found; // used in finding each number 0...(nrfgroups-1)
		int max; // maximum number currently used in group-part

		// Remove eliminated groups
		i = 0;
		while (i < nrOfGroups)
			if (groups[i] == Constants.ELIMINATED) {
				for (j = i; j < nrOfGroups - 1; j++)
					groups[j] = groups[j + 1];
				nrOfGroups--;
			} else
				i++;

		// Renumber to get a nice permutation of 0,1,2,3... again
		i = 0;
		while (i < nrOfGroups) {
			j = 0;
			found = false;
			max = 0;
			// Look for the current (i) number
			while ((j < nrOfGroups) && (!found)) {
				if (groups[j] > groups[max])
					max = j;
				if (groups[j] == i)
					found = true;
				else
					j++;
			}

			// Number (i) not found, give new number to largest number
			if (!found) {
				for (j = 0; j < nrOfObjects; j++)
					if (objects[j] == groups[max])
						objects[j] = i;
				groups[max] = i;
			}
			i++;
		}
	} // CompactGroup ()
}