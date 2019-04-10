package AGCanonico;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.DoubleStream;

import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

class AG_ExtF6  {

	public ArrayList<IndividuoF6> populacao;
	private double probCruz, probMut;
	private int numeroIteracoes, tamanhoPopulacao, nBits, precisao;
	public IndividuoF6 melhorIndividuo;
	public ArrayList<Integer> fitnessPorIteracoes;
	public ArrayList<Double> mediaFitnessPopulacao;
	private Random random;
	public String saidaMelhorIndividuo, saidaIndividuos;
	public double[] execMediaPop, execMelhosInd;

	// Construtor Entrada
	public AG_ExtF6(int tamanhoPopulacao, int numeroIteracoes, double probCruz, double probMut, int nBits, int precisao) {
		this.numeroIteracoes = numeroIteracoes;
		this.tamanhoPopulacao = tamanhoPopulacao;
		this.probCruz = probCruz;
		this.probMut = probMut;
		this.melhorIndividuo = new IndividuoF6();
		this.fitnessPorIteracoes = new ArrayList<Integer>();
		this.mediaFitnessPopulacao = new ArrayList<Double>();
		this.nBits = nBits;
		this.precisao = precisao;
		random = new Random();
		execMediaPop = new double[this.numeroIteracoes];
		execMelhosInd = new double[this.numeroIteracoes];
	}

	// Iniciar
	@SuppressWarnings("unchecked")
	public void run() {

		// Avaliar Populacao
		for (IndividuoF6 individuo : this.populacao) {
			avalia(individuo);
		}

		// Ranqueia populaÃƒÂ§ÃƒÂ£o
		this.rank();

		

		int iteracao = 0;

		while (this.numeroIteracoes > iteracao) {

			ArrayList<IndividuoF6> novaPopulacao = new ArrayList<IndividuoF6>();

			while (novaPopulacao.size() < populacao.size()) {

				// Selecinar cruzamento por Roleta
				IndividuoF6 pai1 = this.selecionaPai();
				IndividuoF6 pai2 = this.selecionaPai();

				// Cruzamento dos pais de acordo com a probabilidade e
				// MutaÃƒÂ§ÃƒÂ£o
				// de acordo com probabilidade
				if (random.nextDouble() <= this.probCruz) {
					// Cruzamento
					IndividuoF6[] filhos = this.filhosCruzamento(pai1, pai2);

					// Mutacao de acordo com probabilidade
					this.mutacao(filhos[0]);
					this.mutacao(filhos[1]);

					novaPopulacao.add(filhos[0]);
					novaPopulacao.add(filhos[1]);

				}
			}

			this.populacao = null;
			this.populacao = (ArrayList<IndividuoF6>) novaPopulacao.clone();
			novaPopulacao.clear();

			double fitnessPopulacao = 0;
			// Avaliar Nova Populacao
			for (IndividuoF6 individuo : this.populacao) {
				avalia(individuo);
				fitnessPopulacao += (double) individuo.getFitness();
			}

			// Ranqueia populacao
			this.rank();

			System.out.println("");
			System.out.println("IteraÃ§ao: " + iteracao);
			double[] XY = this.converteBinarioEmReal(melhorIndividuo.getCromossomo());
			System.out.println("Melhor Individuo: " + melhorIndividuo.getCromossomo() + ": " + XY[0] + " " + XY[1]
					+ " : " + melhorIndividuo.getFitness());

			this.insereNoGrafico(fitnessPopulacao, iteracao);

			iteracao++;

		}
		

	}

	private void insereNoGrafico(double fitnessPopulacao, int iteracao) {

		//double[] XY = this.converteBinarioEmReal(melhorIndividuo.getCromossomo());
		
		execMelhosInd[iteracao] = melhorIndividuo.getFitness();
		
		//gravarArqMelhorInd.printf("\n"+iteracao + ";" + melhorIndividuo.getFitness());

		execMediaPop[iteracao] = fitnessPopulacao / this.populacao.size();
		
		//gravarArqMediaInd.printf("\n" + iteracao +";"+ (fitnessPopulacao / this.populacao.size()));

	}

	// Gerar populaÃƒÂ§ÃƒÂ£o inicial de numeros reais
	private ArrayList<IndividuoF6> populacaoInicialReais(int tamanhoPopulacao) {

		ArrayList<IndividuoF6> populacaoInicial = new ArrayList<IndividuoF6>();

		DoubleStream dsX = new Random().doubles(-100, 100);
		double[] numerosX = dsX.limit(tamanhoPopulacao).toArray();

		DoubleStream dsY = new Random().doubles(-100, 100);
		double[] numerosY = dsY.limit(tamanhoPopulacao).toArray();

		for (int i = 0; i < tamanhoPopulacao; i++) {
			String cromossomo = converteRealEmBinario(numerosX[i]) + converteRealEmBinario(numerosY[i]);

			IndividuoF6 individuo = new IndividuoF6(cromossomo);
			double[] XY = { numerosX[i], numerosY[i] };
			individuo.setValorReal(XY);

			avalia(individuo);

			populacaoInicial.add(individuo);
		}

		return populacaoInicial;
	}
	
	// Gerar populaÃƒÂ§ÃƒÂ£o inicial de numeros reais por arquivo
		private ArrayList<IndividuoF6> populacaoInicialReais(int tamanhoPopulacao, Scanner f) {

			ArrayList<IndividuoF6> populacaoInicial = new ArrayList<IndividuoF6>();

			

			for (int i = 0; i < tamanhoPopulacao; i++) {
				
				String[] s = f.nextLine().split(" ");
				double x = Double.parseDouble(s[0]);
				double y = Double.parseDouble(s[1]);
				
				String cromossomo = converteRealEmBinario(x) + converteRealEmBinario(y);

				IndividuoF6 individuo = new IndividuoF6(cromossomo);
				double[] XY = { x, y };
				individuo.setValorReal(XY);

				avalia(individuo);

				populacaoInicial.add(individuo);
			}

			return populacaoInicial;
		}

	// SeleÃƒÂ§ÃƒÂ£o
	private IndividuoF6 selecionaPai() {

		int somatorio = 0;
		double probabilidades = 0.0;
		IndividuoF6 pai = null;

		for (IndividuoF6 individuo : this.populacao) {
			somatorio += individuo.getFitness();
		}

		// Roleta
		double ponteiro = random.nextDouble();

		for (IndividuoF6 individuo : this.populacao) {
			if (ponteiro <= ((double) individuo.getFitness() / (double) somatorio) + probabilidades) {
				pai = new IndividuoF6(individuo.getFitness(), individuo.getCromossomo());
				break;
			} else {
				probabilidades += ((double) individuo.getFitness() / (double) somatorio);
			}
		}

		return pai;
	}

	// Cruzamento
	private IndividuoF6[] filhosCruzamento(IndividuoF6 pai1, IndividuoF6 pai2) {

		IndividuoF6 filho1 = new IndividuoF6();
		IndividuoF6 filho2 = new IndividuoF6();

		// Random r = new Random();
		int posicao = random.nextInt(pai1.getCromossomo().length());

		if (pai1.getCromossomo().length() != pai2.getCromossomo().length()) {
			System.out.println("teste");
		}

		String crom1 = pai1.getCromossomo().substring(0, posicao);
		crom1 += pai2.getCromossomo().substring(posicao, pai2.getCromossomo().length());

		String crom2 = pai2.getCromossomo().substring(0, posicao);
		crom2 += pai1.getCromossomo().substring(posicao, pai1.getCromossomo().length());

		filho1.setCromossomo(crom1);
		filho2.setCromossomo(crom2);

		IndividuoF6[] filhos = { filho1, filho2 };

		return filhos;
	}

	// MutaÃƒÂ§ÃƒÂ£o
	private void mutacao(IndividuoF6 filho) {

		char[] crom = filho.getCromossomo().toCharArray();

		for (int posicao = 0; posicao < crom.length; posicao++) {

			if (random.nextDouble() < this.probMut) {
				if (crom[posicao] == '0') {
					crom[posicao] = '1';
				} else if (crom[posicao] == '1') {
					crom[posicao] = '0';
				}

			}
		}

		filho.setCromossomo(String.valueOf(crom));

	}

	// Avaliacao
	private void avalia(IndividuoF6 individuo) {
		String cromossomo = individuo.getCromossomo();

		double[] valoresXY = converteBinarioEmReal(cromossomo);

		double pontuacao = ScafferF6(valoresXY[0], valoresXY[1]);

		individuo.setFitness(pontuacao);
		individuo.setValorReal(valoresXY);
	}

	// Scaffer's F6 function
	private double ScafferF6(double x, double y) {
		double temp1 = x * x + y * y;
		double temp2 = Math.sin(Math.sqrt(temp1));
		double temp3 = 1.0 + 0.001 * temp1;
		return (0.5 + ((temp2 * temp2 - 0.5) / (temp3 * temp3)));
	}

	// Expanded Scaffer's F6 function
	private double EScafferF6(double[] x) {
		double sum = 0.0;
		for (int i = 1; i < x.length; i++) {
			sum += ScafferF6(x[i - 1], x[i]);
		}
		sum += ScafferF6(x[x.length - 1], x[0]);
		return (sum);
	}

	// Ranqueamento da populaÃƒÂ§ÃƒÂ£o
	public void rank() {

		Collections.sort(this.populacao);

		// if (this.melhorIndividuo.getFitness() <
		// populacao.get(0).getFitness()){
		this.melhorIndividuo = new IndividuoF6(populacao.get(0).getFitness(), populacao.get(0).getCromossomo());
		// }
	}

	public String converteRealEmBinario(double numero) {
		String bin = "";
		int inteiro = (int) (numero * Math.pow(10, precisao));

		if (inteiro < 0) {
			inteiro = inteiro * -1;
		}

		System.out.println(inteiro);

		while ((int) inteiro > 0) {
			if (inteiro % 2 == 0)
				bin = "0" + bin;
			else
				bin = "1" + bin;
			inteiro /= 2;

		}

		if (bin.length() < nBits) {
			for (int i = bin.length(); i < nBits; i++) {
				bin = "0" + bin;
			}
		}

		bin = (numero < 0) ? "1" + bin : "0" + bin;

		return bin;

	}

	public double[] converteBinarioEmReal(String binario) {

		String binX = String.copyValueOf(binario.toCharArray(), 0, (binario.length() / 2) - 1);

		String binY = String.copyValueOf(binario.toCharArray(), (binario.length() / 2), (binario.length() / 2) - 1);

		String binX1 = String.copyValueOf(binX.toCharArray(), 1, (binX.length() - 1));
		String binY1 = String.copyValueOf(binY.toCharArray(), 1, (binY.length() - 1));

		double numeroX = Integer.parseInt(binX1, 2);
		double numeroY = Integer.parseInt(binY1, 2);

		if (binX.startsWith("1")) {
			numeroX = numeroX * -1;
		}

		if (binY.startsWith("1")) {
			numeroY = numeroY * -1;
		}

		double[] retorno = { numeroX / Math.pow(10, precisao), numeroY / Math.pow(10, precisao) };

		return retorno;

	}

	public static void main(String args[]) {

		double[] mutacoes = { 0.01 };
		double[] cruzamentos = { 0.75 };
		int[] populacoes = { 50 };
		int nIteracoes = 500;

					// Populacao
					for (int l = 0; l < 5; l++) {

						FileWriter arqMediaInd = null, arqMelhorInd = null;
						PrintWriter gravarArqMediaInd, gravarArqMelhorInd;
						try {
							arqMediaInd = new FileWriter("AlgoritmoGenetic_2\\pop"+l+"\\MediaPopulaco_pop " + l+".csv");
							arqMelhorInd = new FileWriter("AlgoritmoGenetic_2\\pop"+l+"\\MelhorIndividuos_pop " + l+".csv");
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						gravarArqMediaInd = new PrintWriter(arqMediaInd);
						gravarArqMelhorInd = new PrintWriter(arqMelhorInd);
						
						ArrayList<double[]> mediaPopExecs = new ArrayList<double[]>();
						ArrayList<double[]> melhorIndExecs = new ArrayList<double[]>();

						// Execucoes da população
						for (int k = 0; k < 5; k++) {
							// 28 bits para representar os numero -100,100 com 6
							// casas decimais
							// quando converter para real, considerar o piso igual a
							// -100
							AG_ExtF6 ag = new AG_ExtF6(populacoes[0], nIteracoes, cruzamentos[0], mutacoes[0], 28, 6);

							FileReader file = null;
							try {
								file = new FileReader("AlgoritmoGenetic_2\\PopulacoInicial_" + l +  " _Execucao.txt");
							} catch (FileNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							Scanner f = new Scanner(file);
							ag.populacao = ag.populacaoInicialReais(populacoes[0], f);
							f.close();
							//String saidaIndividuos = "AlgoritmoGenetic_2\\pop"+l+"\\MediaPopulaco_pop " + l
							//		+ "Execucao "+ k + ".csv";
							//String melhoresIndividuos = "AlgoritmoGenetic_2\\pop"+l+"\\MelhorIndividuos_pop " + l
							//		+ "Execucao" + k + ".csv";

							//ag.saidaIndividuos = saidaIndividuos;
							//ag.saidaMelhorIndividuo = melhoresIndividuos;

							ag.run();
							
							mediaPopExecs.add(ag.execMediaPop);
							melhorIndExecs.add(ag.execMelhosInd);
							
							//Thread t = new Thread(ag);
							// Cria a linha de execuÃ§Ã£o
							//t.start(); // Ativa a thread
						}
						gravarArqMediaInd.printf("iterações;E1;E2;E3;E4;E5");
						gravarArqMelhorInd.printf("iterações;E1;E2;E3;E4;E5");
						for (int i = 0; i < nIteracoes; i++) {
							gravarArqMediaInd.printf("\n"+i + ";" + mediaPopExecs.get(0)[i]+";"+ mediaPopExecs.get(1)[i]+";"+ mediaPopExecs.get(2)[i]+";"+ mediaPopExecs.get(3)[i]+";"+ mediaPopExecs.get(4)[i]+";");
							gravarArqMelhorInd.printf("\n"+i + ";" + melhorIndExecs.get(0)[i]+";"+ melhorIndExecs.get(1)[i]+";"+ melhorIndExecs.get(2)[i]+";"+ melhorIndExecs.get(3)[i]+";"+ melhorIndExecs.get(4)[i]+";");
						}
						
						try {
							
							gravarArqMelhorInd.close();
							arqMelhorInd.close();
							gravarArqMediaInd.close();
							arqMediaInd.close();
							
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}


		/*
		 * AG ag = new AG(10, 1000, "1010101010101010", 1.0, 0.01, String
		 * titulo, String nomeGrafico);
		 * 
		 * JFrame frame = new
		 * JFrame("GrÃ¡fico de Linha do Algoritmo GenÃ©tico");
		 * frame.add(ag.getPanel());
		 * 
		 * frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); frame.pack();
		 * frame.setSize(1024, 480); frame.setVisible(true);
		 * 
		 * Thread t = new Thread(ag); //Cria a linha de execuÃ§Ã£o t.start();
		 * //Ativa a thread
		 * 
		 */
	}

}

class IndividuoF6 implements Comparable<IndividuoF6> {

	private double fitness = 0;
	private String cromossomo;
	private double[] XY;

	public IndividuoF6() {
	}

	public IndividuoF6(String cromossomo) {
		this.cromossomo = cromossomo;
	}

	public IndividuoF6(double fitness, String cromossomo) {
		super();
		this.fitness = fitness;
		this.cromossomo = cromossomo;
	}

	public void setValorReal(double[] XY) {
		this.XY = XY;
	}

	public double[] getValorReal() {
		return this.XY;
	}

	public double getFitness() {
		return fitness;
	}

	public void setFitness(double fitness) {
		this.fitness = fitness;
	}

	public String getCromossomo() {
		return cromossomo;
	}

	public void setCromossomo(String cromossomo) {
		this.cromossomo = cromossomo;
	}

	@Override
	public int compareTo(IndividuoF6 outroIndividuo) {
		// TODO Auto-generated method stub
		if (this.fitness > outroIndividuo.getFitness()) {
			return -1;
		}
		if (this.fitness < outroIndividuo.getFitness()) {
			return 1;
		}
		return 0;
	}

}
