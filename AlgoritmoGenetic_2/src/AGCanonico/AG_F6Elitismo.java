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

class AG_F6Elitismo {

	public ArrayList<IndividuoElitismo> populacao;
	public long seed;
	private double probCruz, probMut, percentualElitismo;
	private int numeroIteracoes, tamanhoPopulacao, nBits, precisao, tipoElitismo;
	public IndividuoElitismo melhorIndividuo, piorIndividuo;
	public ArrayList<Integer> fitnessPorIteracoes;
	public ArrayList<Double> mediaFitnessPopulacao;
	private Random random;
	public String saidaMelhorIndividuo, saidaIndividuos, avaliacao;
	public double[] execMediaPop, execMelhosInd, execPioresInd;
	double[] minMaxNorm;
	private boolean normalizado;

	// Construtor Entrada
	public AG_F6Elitismo(int tamanhoPopulacao, int numeroIteracoes, double probCruz, double probMut, int nBits, int precisao,
			String avaliacao, long seed, double[] maxMinNorm, boolean normalizado, int tipoElitismo,
			double percentualElitismo) {
		this.numeroIteracoes = numeroIteracoes;
		this.tamanhoPopulacao = tamanhoPopulacao;
		this.probCruz = probCruz;
		this.probMut = probMut;
		this.melhorIndividuo = new IndividuoElitismo();
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
		this.tipoElitismo = tipoElitismo;
		this.percentualElitismo = percentualElitismo;
	}

	// Iniciar
	@SuppressWarnings("unchecked")
	public void run() {

		double fitnessPopulacao = 0;

		// Avaliar Populacao
		for (IndividuoElitismo individuo : this.populacao) {
			avalia(individuo, avaliacao);
			fitnessPopulacao += (double) individuo.getFitness();
		}

		// Ranqueia população
		this.rank();

		this.insereNoGrafico(fitnessPopulacao, 0);

		int iteracao = 1;

		while (this.numeroIteracoes + 1 > iteracao) {

			ArrayList<IndividuoElitismo> novaPopulacao = new ArrayList<IndividuoElitismo>();

			while (novaPopulacao.size() < populacao.size()) {

				IndividuoElitismo pai1 = null;
				IndividuoElitismo pai2 = null;

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
				// Mutação
				// de acordo com probabilidade
				if (random.nextDouble() <= this.probCruz) {
					// Cruzamento
					IndividuoElitismo[] filhos = this.filhosCruzamento(pai1, pai2);

					// Mutacao de acordo com probabilidade
					this.mutacao(filhos[0]);
					this.mutacao(filhos[1]);

					novaPopulacao.add(filhos[0]);
					novaPopulacao.add(filhos[1]);

				}
			}

			this.elitismo(novaPopulacao);

			fitnessPopulacao = 0;
			// Avaliar Nova Populacao
			for (IndividuoElitismo individuo : this.populacao) {
				avalia(individuo, this.avaliacao);
				fitnessPopulacao += (double) individuo.getFitness();
			}

			// Ranqueia populacao
			this.rank();
			/*
			 * System.out.println(""); System.out.println("Itera�ao: " + iteracao); double[]
			 * XY = this.converteBinarioEmReal(melhorIndividuo.getCromossomo());
			 * System.out.println("Melhor Individuo: " + melhorIndividuo.getCromossomo() +
			 * ": " + XY[0] + " " + XY[1] + " : " + melhorIndividuo.getFitness());
			 */
			this.insereNoGrafico(fitnessPopulacao, iteracao);

			iteracao++;

		}

	}

	private void elitismo(ArrayList<IndividuoElitismo> novaPopulacao) {
		if (tipoElitismo == 0) {
			this.novaPopSemElitismo(novaPopulacao);
		} else if (tipoElitismo == 1) {
			this.novaPopElitista(novaPopulacao);
		} else if (tipoElitismo == 2) {
			this.novaPopEstadoEstacionario(novaPopulacao);
		}
	}

	private void novaPopSemElitismo(ArrayList<IndividuoElitismo> novaPopulacao) {
		this.populacao = null;
		this.populacao = (ArrayList<IndividuoElitismo>) novaPopulacao.clone();
		novaPopulacao.clear();
	}

	private void novaPopElitista(ArrayList<IndividuoElitismo> novaPopulacao) {
		IndividuoElitismo melhorIndividuo = new IndividuoElitismo(this.populacao.get(0).getFitness(),
				this.populacao.get(0).getCromossomo());
		this.populacao = null;
		this.populacao = (ArrayList<IndividuoElitismo>) novaPopulacao.clone();
		// Ranqueia populacao
		this.rank();
		this.populacao.set(this.populacao.size() - 1, melhorIndividuo);
		novaPopulacao.clear();
	}

	private void novaPopEstadoEstacionario(ArrayList<IndividuoElitismo> novaPopulacao) {

		int quantidade = (int) (this.percentualElitismo / 100) * this.populacao.size();

		ArrayList<IndividuoElitismo> melhores = new ArrayList<IndividuoElitismo>();

		for (int i = 0; i < quantidade; i++) {
			melhores.add(new IndividuoElitismo(this.populacao.get(i).getFitness(), this.populacao.get(i).getCromossomo()));
		}

		this.populacao = null;
		this.populacao = (ArrayList<IndividuoElitismo>) novaPopulacao.clone();
		// Ranqueia populacao
		this.rank();

		for (int i = this.populacao.size() - quantidade; i < this.populacao.size(); i++) {
			this.populacao.set(i, melhores.get(this.populacao.size() - i - 1));
		}

		this.populacao.set(this.populacao.size() - 1, melhorIndividuo);
		novaPopulacao.clear();

	}

	private void insereNoGrafico(double fitnessPopulacao, int iteracao) {

		execMelhosInd[iteracao] = melhorIndividuo.getFitness();

		execPioresInd[iteracao] = piorIndividuo.getFitness();

		execMediaPop[iteracao] = fitnessPopulacao / this.populacao.size();
	}

	// Gerar população inicial de numeros reais
	private ArrayList<IndividuoElitismo> populacaoInicialReais(int tamanhoPopulacao) {

		ArrayList<IndividuoElitismo> populacaoInicial = new ArrayList<IndividuoElitismo>();

		DoubleStream dsX = random.doubles(-100, 100);
		double[] numerosX = dsX.limit(tamanhoPopulacao).toArray();

		DoubleStream dsY = random.doubles(-100, 100);
		double[] numerosY = dsY.limit(tamanhoPopulacao).toArray();

		for (int i = 0; i < tamanhoPopulacao; i++) {
			String cromossomo = converteRealEmBinario(numerosX[i]) + converteRealEmBinario(numerosY[i]);

			IndividuoElitismo individuo = new IndividuoElitismo(cromossomo);
			double[] XY = { numerosX[i], numerosY[i] };
			individuo.setValorReal(XY);

			avalia(individuo, this.avaliacao);

			populacaoInicial.add(individuo);
		}

		return populacaoInicial;
	}

	// Gerar população inicial de numeros reais por arquivo
	private ArrayList<IndividuoElitismo> populacaoInicialReais(int tamanhoPopulacao, Scanner f) {

		ArrayList<IndividuoElitismo> populacaoInicial = new ArrayList<IndividuoElitismo>();
		// Para saltar a seed, primeira linha do arquivo
		f.nextLine();

		for (int i = 0; i < tamanhoPopulacao; i++) {

			String[] s = f.nextLine().split("\t");
			double x = Double.parseDouble(s[0]);
			double y = Double.parseDouble(s[1]);

			String cromossomo = converteRealEmBinario(x) + converteRealEmBinario(y);

			IndividuoElitismo individuo = new IndividuoElitismo(cromossomo);
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

	// Selecao Roleta
	private IndividuoElitismo selecionaPai() {

		int somatorio = 0;
		double probabilidades = 0.0;
		IndividuoElitismo pai = null;

		for (IndividuoElitismo individuo : this.populacao) {
			somatorio += individuo.getFitness();
		}

		// Roleta
		double ponteiro = random.nextDouble();

		for (int i = 0; i < this.populacao.size(); i++) {
			IndividuoElitismo individuo = this.populacao.get(i);
			if (ponteiro <= ((double) individuo.getFitness() / (double) somatorio) + probabilidades) {
				pai = new IndividuoElitismo(individuo.getFitness(), individuo.getCromossomo());
				break;
			} else {
				probabilidades += ((double) individuo.getFitness() / (double) somatorio);
			}
		}

		return pai;
	}

	// Selecao Normalizada
	private IndividuoElitismo selecionaPai(double[] fitnessNormalizado) {

		int somatorio = 0;
		double probabilidades = 0.0;
		IndividuoElitismo pai = null;

		for (int i = 0; i < this.populacao.size(); i++) {
			somatorio += fitnessNormalizado[i];
		}

		// Roleta
		double ponteiro = random.nextDouble();

		for (int i = 0; i < this.populacao.size(); i++) {
			IndividuoElitismo individuo = this.populacao.get(i);
			if (ponteiro <= ((double) fitnessNormalizado[i] / (double) somatorio) + probabilidades) {
				pai = new IndividuoElitismo(individuo.getFitness(), individuo.getCromossomo());
				break;
			} else {
				probabilidades += ((double) fitnessNormalizado[i] / (double) somatorio);
			}
		}

		return pai;
	}

	// Cruzamento
	private IndividuoElitismo[] filhosCruzamento(IndividuoElitismo pai1, IndividuoElitismo pai2) {

		IndividuoElitismo filho1 = new IndividuoElitismo();
		IndividuoElitismo filho2 = new IndividuoElitismo();

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

		IndividuoElitismo[] filhos = { filho1, filho2 };

		return filhos;
	}

	// Mutação
	private void mutacao(IndividuoElitismo filho) {

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
	private void avalia(IndividuoElitismo individuo, String metodo) {
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

	// Ranqueamento da população
	public void rank() {

		Collections.sort(this.populacao);

		// if (this.melhorIndividuo.getFitness() <
		// populacao.get(0).getFitness()){
		this.melhorIndividuo = new IndividuoElitismo(this.populacao.get(0).getFitness(),
				this.populacao.get(0).getCromossomo());
		this.piorIndividuo = new IndividuoElitismo(this.populacao.get(this.populacao.size() - 1).getFitness(),
				this.populacao.get(this.populacao.size() - 1).getCromossomo());
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
		long[] seeds = { 123456, 654321, 765432, 234567, 987650 };
		int numeroExecucoes = 5, numeroPopulacoes = 5;
		boolean populacaoJaCriada = true;
		String[] avaliacao = { "ScafferF6" };
		double[] maxMinNorm = { 500, 1 };
		boolean[] normalizado = { false };
		int[] tipoElitismo = { 0, 1, 2 };
		int percentualElitismo = 10;

		for (int b = 0; b < tipoElitismo.length; b++) {

			// Executa com o fitness normalizado para cada populacao novamente e
			// para cada execucao
			for (int o = 0; o < normalizado.length; o++) {
				// Populacao
				for (int l = 0; l < numeroPopulacoes; l++) {

					for (int a = 0; a < avaliacao.length; a++) {

						FileWriter arqMediaInd = null, arqMelhorInd = null, arqMelhorPiorInd = null,
								arqMediaMedias = null;
						PrintWriter gravarArqMediaInd, gravarArqMelhorInd, gravarArqMelhorPiorInd, gravarArqMediaMedias;
						try {
							arqMediaInd = new FileWriter(
									"AlgoritmoGenetic_2\\pop" + l + "\\MediaPopulaco_pop " + l + "_" + avaliacao[a]
											+ "_Norm" + normalizado[o] + "_tipoElitismo" + tipoElitismo[b] + ".csv");
							arqMelhorInd = new FileWriter(
									"AlgoritmoGenetic_2\\pop" + l + "\\MelhorIndividuos_pop " + l + "_" + avaliacao[a]
											+ "_Norm" + normalizado[o] + "_tipoElitismo" + tipoElitismo[b] + ".csv");
							arqMelhorPiorInd = new FileWriter("AlgoritmoGenetic_2\\pop" + l
									+ "\\MelhorMediaPiorIndividuos_pop " + l + "_" + avaliacao[a] + "_Norm"
									+ normalizado[o] + "_tipoElitismo" + tipoElitismo[b] + ".csv");
							arqMediaMedias = new FileWriter("AlgoritmoGenetic_2\\pop" + l
									+ "\\Medias_MelhorMediaPiorIndividuos_pop " + l + "_" + avaliacao[a] + "_Norm"
									+ normalizado[o] + "_tipoElitismo" + tipoElitismo[b] + ".csv");
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

						double[] mediaMediaPopExecs = new double[nIteracoes + 1];
						double[] mediaMelhorIndExecs = new double[nIteracoes + 1];
						double[] mediaPiorIndExecs = new double[nIteracoes + 1];

						// Execucoes da populacao
						for (int k = 0; k < numeroExecucoes; k++) {
							// 28 bits para representar os numero -100,100 com 6
							// casas decimais
							// quando converter para real, considerar o piso igual a
							// -100
							AG_F6Elitismo ag = new AG_F6Elitismo(populacoes[0], nIteracoes, cruzamentos[0], mutacoes[0], 28, 6,
									avaliacao[a], seeds[k], maxMinNorm, normalizado[o], tipoElitismo[b],
									percentualElitismo);

							if (populacaoJaCriada) {
								FileReader file = null;
								try {
									file = new FileReader(
											"AlgoritmoGenetic_2\\PopulacoInicial_" + l + " _Execucaoo.txt");
								} catch (FileNotFoundException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								Scanner f = new Scanner(file);
								ag.populacao = ag.populacaoInicialReais(populacoes[0], f);
								f.close();
							} else {
								ag.populacao = ag.populacaoInicialReais(populacoes[0]);
								AG_F6Elitismo.gravaPopulacaoInicial(ag.populacao, l, ag.seed);
							}

							ag.run();

							mediaPopExecs.add(ag.execMediaPop);
							melhorIndExecs.add(ag.execMelhosInd);
							piorIndExecs.add(ag.execPioresInd);

						}
						gravarArqMediaInd.printf("#iteracoes\tE1\tE2\tE3\tE4\tE5");
						gravarArqMelhorInd.printf("#iteracoes\tE1\tE2\tE3\tE4\tE5");
						gravarArqMelhorPiorInd.printf(
								"#iteracoes\tMelhorE1\tMediaE1\tPiorE1\tMelhorE2\tMediaE2\tPiorE2\tMelhorE3\tMediaE3\tPiorE3\tMelhorE4\tMediaE4\tPiorE4\tMelhorE5\tMediaE5\tPiorE5");
						gravarArqMediaMedias.printf("#iteracoes\tMediaMelhor\tMediaMedias\tMediaPior");
						for (int i = 0; i < nIteracoes + 1; i++) {
							gravarArqMediaInd.printf("\n" + i + "\t" + mediaPopExecs.get(0)[i] + "\t"
									+ mediaPopExecs.get(1)[i] + "\t" + mediaPopExecs.get(2)[i] + "\t"
									+ mediaPopExecs.get(3)[i] + "\t" + mediaPopExecs.get(4)[i]);

							gravarArqMelhorInd.printf("\n" + i + "\t" + melhorIndExecs.get(0)[i] + "\t"
									+ melhorIndExecs.get(1)[i] + "\t" + melhorIndExecs.get(2)[i] + "\t"
									+ melhorIndExecs.get(3)[i] + "\t" + melhorIndExecs.get(4)[i]);

							gravarArqMelhorPiorInd.printf("\n" + i + "\t" + melhorIndExecs.get(0)[i] + "\t"
									+ mediaPopExecs.get(0)[i] + "\t" + piorIndExecs.get(0)[i] + "\t"
									+ melhorIndExecs.get(1)[i] + "\t" + mediaPopExecs.get(1)[i] + "\t"
									+ piorIndExecs.get(1)[i] + "\t" + melhorIndExecs.get(2)[i] + "\t"
									+ mediaPopExecs.get(2)[i] + "\t" + piorIndExecs.get(2)[i] + "\t"
									+ melhorIndExecs.get(3)[i] + "\t" + mediaPopExecs.get(3)[i] + "\t"
									+ piorIndExecs.get(3)[i] + "\t" + melhorIndExecs.get(4)[i] + "\t"
									+ mediaPopExecs.get(4)[i] + "\t" + piorIndExecs.get(4)[i]);

							mediaMediaPopExecs[i] = (mediaPopExecs.get(0)[i] + mediaPopExecs.get(1)[i]
									+ mediaPopExecs.get(2)[i] + mediaPopExecs.get(3)[i] + mediaPopExecs.get(4)[i])
									/ numeroExecucoes;
							mediaMelhorIndExecs[i] = (melhorIndExecs.get(0)[i] + melhorIndExecs.get(1)[i]
									+ melhorIndExecs.get(2)[i] + melhorIndExecs.get(3)[i] + melhorIndExecs.get(4)[i])
									/ numeroExecucoes;
							mediaPiorIndExecs[i] = (piorIndExecs.get(0)[i] + piorIndExecs.get(1)[i]
									+ piorIndExecs.get(2)[i] + piorIndExecs.get(3)[i] + piorIndExecs.get(4)[i])
									/ numeroExecucoes;

							gravarArqMediaMedias.printf("\n" + i + "\t" + mediaMelhorIndExecs[i] + "\t"
									+ mediaMediaPopExecs[i] + "\t" + mediaPiorIndExecs[i]);
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
	}

	public static void gravaPopulacaoInicial(ArrayList<IndividuoElitismo> populacao, int l, long seed) {

		FileWriter arquivoPop = null;
		PrintWriter gravarArqPop = null;
		try {
			arquivoPop = new FileWriter("AlgoritmoGenetic_2\\PopulacoInicial_" + l + " _Execucaoo.txt");
			gravarArqPop = new PrintWriter(arquivoPop);
			gravarArqPop.println("Seed\t" + seed);
			for (IndividuoElitismo individuo : populacao) {
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

class IndividuoElitismo implements Comparable<IndividuoElitismo> {

	private double fitness = 0;
	private String cromossomo;
	private double[] XY;

	public IndividuoElitismo() {
	}

	public IndividuoElitismo(String cromossomo) {
		this.cromossomo = cromossomo;
	}

	public IndividuoElitismo(double fitness, String cromossomo) {
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
	public int compareTo(IndividuoElitismo outroIndividuo) {
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