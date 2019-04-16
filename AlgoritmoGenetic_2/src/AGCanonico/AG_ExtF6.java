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

class AG_ExtF6 {

	public ArrayList<IndividuoF6> populacao;
	public long seed;
	private double probCruz, probMut;
	private int numeroIteracoes, tamanhoPopulacao, nBits, precisao;
	public IndividuoF6 melhorIndividuo, piorIndividuo;
	public ArrayList<Integer> fitnessPorIteracoes;
	public ArrayList<Double> mediaFitnessPopulacao;
	private Random random;
	public String saidaMelhorIndividuo, saidaIndividuos, avaliacao;
	public double[] execMediaPop, execMelhosInd, execPioresInd;
	double[] minMaxNorm;
	private boolean normalizado;

	// Construtor Entrada
	public AG_ExtF6(int tamanhoPopulacao, int numeroIteracoes, double probCruz, double probMut, int nBits, int precisao,
			String avaliacao, long seed, double[] maxMinNorm, boolean normalizado) {
		this.numeroIteracoes = numeroIteracoes;
		this.tamanhoPopulacao = tamanhoPopulacao;
		this.probCruz = probCruz;
		this.probMut = probMut;
		this.melhorIndividuo = new IndividuoF6();
		this.fitnessPorIteracoes = new ArrayList<Integer>();
		this.mediaFitnessPopulacao = new ArrayList<Double>();
		this.nBits = nBits;
		this.precisao = precisao;
		this.seed = seed;
		random = new Random(this.seed);
		execMediaPop = new double[this.numeroIteracoes + 1];
		execMelhosInd = new double[this.numeroIteracoes + 1];
		execPioresInd = new double[this.numeroIteracoes + 1];
		this.avaliacao = avaliacao;
		this.minMaxNorm = maxMinNorm;
		this.normalizado = normalizado;
	}

	// Iniciar
	@SuppressWarnings("unchecked")
	public void run() {

		double fitnessPopulacao = 0;

		// Avaliar Populacao
		for (IndividuoF6 individuo : this.populacao) {
			avalia(individuo, avaliacao);
			fitnessPopulacao += (double) individuo.getFitness();
		}

		// Ranqueia populaÃ§Ã£o
		this.rank();

		this.insereNoGrafico(fitnessPopulacao, 0);

		int iteracao = 1;

		while (this.numeroIteracoes + 1 > iteracao) {

			ArrayList<IndividuoF6> novaPopulacao = new ArrayList<IndividuoF6>();

			while (novaPopulacao.size() < populacao.size()) {

				IndividuoF6 pai1 = null;
				IndividuoF6 pai2 = null;

				if (this.normalizado) {
					// Selecinar cruzamento por Roleta Normalizada
					double[] fitnessNormalizado = this.normalizaAval();
					pai1 = this.selecionaPai(fitnessNormalizado);
					pai2 = this.selecionaPai(fitnessNormalizado);
				} else {
					// Selecinar cruzamento por Roleta
					pai1 = this.selecionaPai();
					pai2 = this.selecionaPai();
				}
				// Cruzamento dos pais de acordo com a probabilidade e
				// MutaÃ§Ã£o
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

			fitnessPopulacao = 0;
			// Avaliar Nova Populacao
			for (IndividuoF6 individuo : this.populacao) {
				avalia(individuo, this.avaliacao);
				fitnessPopulacao += (double) individuo.getFitness();
			}

			// Ranqueia populacao
			this.rank();
/*
			System.out.println("");
			System.out.println("Iteraçao: " + iteracao);
			double[] XY = this.converteBinarioEmReal(melhorIndividuo.getCromossomo());
			System.out.println("Melhor Individuo: " + melhorIndividuo.getCromossomo() + ": " + XY[0] + " " + XY[1]
					+ " : " + melhorIndividuo.getFitness());
*/
			this.insereNoGrafico(fitnessPopulacao, iteracao);

			iteracao++;

		}

	}

	private void insereNoGrafico(double fitnessPopulacao, int iteracao) {

		execMelhosInd[iteracao] = melhorIndividuo.getFitness();

		execPioresInd[iteracao] = piorIndividuo.getFitness();

		execMediaPop[iteracao] = fitnessPopulacao / this.populacao.size();
	}

	// Gerar populaÃ§Ã£o inicial de numeros reais
	private ArrayList<IndividuoF6> populacaoInicialReais(int tamanhoPopulacao) {

		ArrayList<IndividuoF6> populacaoInicial = new ArrayList<IndividuoF6>();

		DoubleStream dsX = random.doubles(-100, 100);
		double[] numerosX = dsX.limit(tamanhoPopulacao).toArray();

		DoubleStream dsY = random.doubles(-100, 100);
		double[] numerosY = dsY.limit(tamanhoPopulacao).toArray();

		for (int i = 0; i < tamanhoPopulacao; i++) {
			String cromossomo = converteRealEmBinario(numerosX[i]) + converteRealEmBinario(numerosY[i]);

			IndividuoF6 individuo = new IndividuoF6(cromossomo);
			double[] XY = { numerosX[i], numerosY[i] };
			individuo.setValorReal(XY);

			avalia(individuo, this.avaliacao);

			populacaoInicial.add(individuo);
		}

		return populacaoInicial;
	}

	// Gerar populaÃ§Ã£o inicial de numeros reais por arquivo
	private ArrayList<IndividuoF6> populacaoInicialReais(int tamanhoPopulacao, Scanner f) {

		ArrayList<IndividuoF6> populacaoInicial = new ArrayList<IndividuoF6>();
		// Para saltar a seed, primeira linha do arquivo
		f.nextLine();

		for (int i = 0; i < tamanhoPopulacao; i++) {

			String[] s = f.nextLine().split("\t");
			double x = Double.parseDouble(s[0]);
			double y = Double.parseDouble(s[1]);

			String cromossomo = converteRealEmBinario(x) + converteRealEmBinario(y);

			IndividuoF6 individuo = new IndividuoF6(cromossomo);
			double[] XY = { x, y };
			individuo.setValorReal(XY);

			avalia(individuo, this.avaliacao);

			populacaoInicial.add(individuo);
		}

		return populacaoInicial;
	}

	private double[] normalizaAval() {
		double[] novosFitness = new double[this.populacao.size()];

		for (int i = 0; i < novosFitness.length; i++) {
			novosFitness[novosFitness.length - i - 1] = this.minMaxNorm[0]
					+ ((this.minMaxNorm[1] - this.minMaxNorm[0]) / (this.populacao.size() - 1)) * i - 1;
		}

		return novosFitness;
	}

	// Sele褯 Roleta
	private IndividuoF6 selecionaPai() {

		int somatorio = 0;
		double probabilidades = 0.0;
		IndividuoF6 pai = null;

		for (IndividuoF6 individuo : this.populacao) {
			somatorio += individuo.getFitness();
		}

		// Roleta
		double ponteiro = random.nextDouble();

		for (int i = 0; i < this.populacao.size(); i++) {
			IndividuoF6 individuo = this.populacao.get(i);
			if (ponteiro <= ((double) individuo.getFitness() / (double) somatorio) + probabilidades) {
				pai = new IndividuoF6(individuo.getFitness(), individuo.getCromossomo());
				break;
			} else {
				probabilidades += ((double) individuo.getFitness() / (double) somatorio);
			}
		}

		return pai;
	}

	// Sele褯 Normalizada
	private IndividuoF6 selecionaPai(double[] fitnessNormalizado) {

		int somatorio = 0;
		double probabilidades = 0.0;
		IndividuoF6 pai = null;

		for (int i = 0; i < this.populacao.size(); i++) {
			somatorio += fitnessNormalizado[i];
		}

		// Roleta
		double ponteiro = random.nextDouble();

		for (int i = 0; i < this.populacao.size(); i++) {
			IndividuoF6 individuo = this.populacao.get(i);
			if (ponteiro <= ((double) fitnessNormalizado[i] / (double) somatorio) + probabilidades) {
				pai = new IndividuoF6(individuo.getFitness(), individuo.getCromossomo());
				break;
			} else {
				probabilidades += ((double) fitnessNormalizado[i] / (double) somatorio);
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

	// MutaÃ§Ã£o
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
	private void avalia(IndividuoF6 individuo, String metodo) {
		String cromossomo = individuo.getCromossomo();

		double[] valoresXY = converteBinarioEmReal(cromossomo);

		double pontuacao = Integer.MIN_VALUE;

		if (metodo.equals("ScafferF6")) {
			pontuacao = ScafferF6(valoresXY[0], valoresXY[1]);
		} else
			pontuacao = ScafferF6_M(valoresXY[0], valoresXY[1]);

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

	// Scaffer's F6 function Modificada
	private double ScafferF6_M(double x, double y) {
		double temp1 = x * x + y * y;
		double temp2 = Math.sin(Math.sqrt(temp1));
		double temp3 = 1.0 + 0.001 * temp1;
		return (999.5 + ((temp2 * temp2 - 0.5) / (temp3 * temp3)));
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

	// Ranqueamento da populaÃ§Ã£o
	public void rank() {

		Collections.sort(this.populacao);

		// if (this.melhorIndividuo.getFitness() <
		// populacao.get(0).getFitness()){
		this.melhorIndividuo = new IndividuoF6(this.populacao.get(0).getFitness(), this.populacao.get(0).getCromossomo());
		this.piorIndividuo = new IndividuoF6(this.populacao.get(this.populacao.size()-1).getFitness(), this.populacao.get(this.populacao.size()-1).getCromossomo());
		// }
	}

	public String converteRealEmBinario(double numero) {
		String bin = "";
		int inteiro = (int) (numero * Math.pow(10, precisao));

		if (inteiro < 0) {
			inteiro = inteiro * -1;
		}

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
		String[] avaliacao = { "ScafferF6", "ScafferF6_M" };
		long[] seeds = { 123456, 654321, 765432, 234567, 987650 };
		int numeroExecucoes = 5, numeroPopulacoes = 5;
		boolean populacaoJaCriada = true;
		double[] maxMinNorm = { 500, 1 };
		boolean[] normalizado = { false, true };

		// Executa com o fitness normalizado para cada popula褯 novamente e
		// para cada execu褯
		for (int o = 0; o < normalizado.length; o++) {
			// Populacao
			for (int l = 0; l < numeroPopulacoes; l++) {

				for (int a = 0; a < avaliacao.length; a++) {

					FileWriter arqMediaInd = null, arqMelhorInd = null, arqMelhorPiorInd = null, arqMediaMedias = null;
					PrintWriter gravarArqMediaInd, gravarArqMelhorInd, gravarArqMelhorPiorInd, gravarArqMediaMedias;
					try {
						arqMediaInd = new FileWriter("AlgoritmoGenetic_2\\pop" + l + "\\MediaPopulaco_pop " + l + "_"
								+ avaliacao[a] + "_Norm" + normalizado[o] + ".csv");
						arqMelhorInd = new FileWriter("AlgoritmoGenetic_2\\pop" + l + "\\MelhorIndividuos_pop " + l
								+ "_" + avaliacao[a] + "_Norm" + normalizado[o] + ".csv");
						arqMelhorPiorInd = new FileWriter("AlgoritmoGenetic_2\\pop" + l + "\\MelhorMediaPiorIndividuos_pop " + l
								+ "_" + avaliacao[a] + "_Norm" + normalizado[o] + ".csv");
						arqMediaMedias = new FileWriter("AlgoritmoGenetic_2\\pop" + l + "\\Medias_MelhorMediaPiorIndividuos_pop " + l
								+ "_" + avaliacao[a] + "_Norm" + normalizado[o] + ".csv");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					gravarArqMediaInd = new PrintWriter(arqMediaInd);
					gravarArqMelhorInd = new PrintWriter(arqMelhorInd);
					gravarArqMelhorPiorInd = new PrintWriter(arqMelhorPiorInd);
					gravarArqMediaMedias = new PrintWriter(arqMediaMedias);

					ArrayList<double[]> mediaPopExecs = new ArrayList<double[]>();
					ArrayList<double[]> melhorIndExecs = new ArrayList<double[]>();
					ArrayList<double[]> piorIndExecs = new ArrayList<double[]>();

					double[] mediaMediaPopExecs = new double[nIteracoes+1];
					double[] mediaMelhorIndExecs = new double[nIteracoes+1];
					double[] mediaPiorIndExecs = new double[nIteracoes+1];
					
					// Execucoes da popula褯
					for (int k = 0; k < numeroExecucoes; k++) {
						// 28 bits para representar os numero -100,100 com 6
						// casas decimais
						// quando converter para real, considerar o piso igual a
						// -100
						AG_ExtF6 ag = new AG_ExtF6(populacoes[0], nIteracoes, cruzamentos[0], mutacoes[0], 28, 6,
								avaliacao[a], seeds[k], maxMinNorm, normalizado[o]);

						if (populacaoJaCriada) {
							FileReader file = null;
							try {
								file = new FileReader("AlgoritmoGenetic_2\\PopulacoInicial_" + l + " _Execucaoo.txt");
							} catch (FileNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							Scanner f = new Scanner(file);
							ag.populacao = ag.populacaoInicialReais(populacoes[0], f);
							f.close();
						} else {
							ag.populacao = ag.populacaoInicialReais(populacoes[0]);
							AG_ExtF6.gravaPopulacaoInicial(ag.populacao, l, ag.seed);
						}

						ag.run();

						mediaPopExecs.add(ag.execMediaPop);
						melhorIndExecs.add(ag.execMelhosInd);
						piorIndExecs.add(ag.execPioresInd);

					}
					gravarArqMediaInd.printf("#itera趥s\tE1\tE2\tE3\tE4\tE5");
					gravarArqMelhorInd.printf("#itera趥s\tE1\tE2\tE3\tE4\tE5");
					gravarArqMelhorPiorInd.printf("#itera趥s\tMelhorE1\tMediaE1\tPiorE1\tMelhorE2\tMediaE2\tPiorE2\tMelhorE3\tMediaE3\tPiorE3\tMelhorE4\tMediaE4\tPiorE4\tMelhorE5\tMediaE5\tPiorE5");
					gravarArqMediaMedias.printf("#itera趥s\tMediaMelhor\tMediaMedias\tMediaPior");
					for (int i = 0; i < nIteracoes + 1; i++) {
						gravarArqMediaInd.printf("\n" + i + "\t" + mediaPopExecs.get(0)[i] + "\t"
								+ mediaPopExecs.get(1)[i] + "\t" + mediaPopExecs.get(2)[i] + "\t"
								+ mediaPopExecs.get(3)[i] + "\t" + mediaPopExecs.get(4)[i]);
						
						gravarArqMelhorInd.printf("\n" + i + "\t" + melhorIndExecs.get(0)[i] + "\t"
								+ melhorIndExecs.get(1)[i] + "\t" + melhorIndExecs.get(2)[i] + "\t"
								+ melhorIndExecs.get(3)[i] + "\t" + melhorIndExecs.get(4)[i]);
						
						gravarArqMelhorPiorInd.printf("\n" + i + "\t" + melhorIndExecs.get(0)[i] + "\t" + mediaPopExecs.get(0)[i] + "\t" + piorIndExecs.get(0)[i] + "\t"
								+ melhorIndExecs.get(1)[i] + "\t" + mediaPopExecs.get(1)[i] + "\t" + piorIndExecs.get(1)[i] + "\t"
								+ melhorIndExecs.get(2)[i] + "\t" + mediaPopExecs.get(2)[i] + "\t" + piorIndExecs.get(2)[i] + "\t"
								+ melhorIndExecs.get(3)[i] + "\t" + mediaPopExecs.get(3)[i] + "\t" + piorIndExecs.get(3)[i] + "\t"
								+ melhorIndExecs.get(4)[i] + "\t" + mediaPopExecs.get(4)[i] + "\t" + piorIndExecs.get(4)[i]);
						
						mediaMediaPopExecs[i] = (mediaPopExecs.get(0)[i] + mediaPopExecs.get(1)[i] + mediaPopExecs.get(2)[i] + mediaPopExecs.get(3)[i] + mediaPopExecs.get(4)[i])/numeroExecucoes;
						mediaMelhorIndExecs[i] = (melhorIndExecs.get(0)[i] + melhorIndExecs.get(1)[i] + melhorIndExecs.get(2)[i] + melhorIndExecs.get(3)[i] + melhorIndExecs.get(4)[i])/numeroExecucoes;
						mediaPiorIndExecs[i] = (piorIndExecs.get(0)[i] + piorIndExecs.get(1)[i] + piorIndExecs.get(2)[i] + piorIndExecs.get(3)[i] + piorIndExecs.get(4)[i])/numeroExecucoes;
						
						gravarArqMediaMedias.printf("\n" + i + "\t" + mediaMelhorIndExecs[i] + "\t" + mediaMediaPopExecs[i] + "\t" + mediaPiorIndExecs[i]);
					}

					try {

						gravarArqMelhorInd.close();
						arqMelhorInd.close();
						gravarArqMediaInd.close();
						arqMediaInd.close();
						gravarArqMelhorPiorInd.close();
						arqMelhorPiorInd.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

	public static void gravaPopulacaoInicial(ArrayList<IndividuoF6> populacao, int l, long seed) {

		FileWriter arquivoPop = null;
		PrintWriter gravarArqPop = null;
		try {
			arquivoPop = new FileWriter("AlgoritmoGenetic_2\\PopulacoInicial_" + l + " _Execucaoo.txt");
			gravarArqPop = new PrintWriter(arquivoPop);
			gravarArqPop.println("Seed\t" + seed);
			for (IndividuoF6 individuo : populacao) {
				gravarArqPop.println(individuo.getValorReal()[0] + "\t" + String.valueOf(individuo.getValorReal()[1]));
			}

			gravarArqPop.close();
			arquivoPop.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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
