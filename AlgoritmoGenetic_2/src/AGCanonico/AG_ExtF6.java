package AGCanonico;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.stream.DoubleStream;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

class AG_ExtF6 implements Runnable {

	private ArrayList<Individuo> populacao;
	private double probCruz, probMut;
	private int numeroIteracoes, tamanhoPopulacao, nBits, precisao;
	public Individuo melhorIndividuo;
	public ArrayList<Integer> fitnessPorIteracoes;
	public ArrayList<Double> mediaFitnessPopulacao;
	private DefaultCategoryDataset ds;
	private JFreeChart grafico;
	private String nomeGrafico, tituloGrafico, iteracaoFinal = "";
	private Random random;

	// Construtor Entrada
	public AG_ExtF6(int tamanhoPopulacao, int numeroIteracoes, double probCruz, double probMut, String tituloGrafico,
			String nomeGrafico, int nBits, int precisao) {
		this.numeroIteracoes = numeroIteracoes;
		this.tamanhoPopulacao = tamanhoPopulacao;
		this.probCruz = probCruz;
		this.probMut = probMut;
		this.melhorIndividuo = new Individuo();
		this.fitnessPorIteracoes = new ArrayList<Integer>();
		this.mediaFitnessPopulacao = new ArrayList<Double>();
		this.ds = new DefaultCategoryDataset();
		this.nomeGrafico = nomeGrafico;
		this.tituloGrafico = tituloGrafico;
		this.nBits = nBits;
		this.precisao = precisao;
	}

	// Iniciar
	@SuppressWarnings("unchecked")
	public void run() {
		// Iniciar PopulaÃ§Ã£o
		this.populacao = this.populacaoInicialReais();

		// Avaliar Populacao
		for (Individuo individuo : this.populacao) {
			avalia(individuo);
		}
		
		// Ranqueia populaÃ§Ã£o
		this.rank();

		random = new Random(123456);

		int iteracao = 0;

		while (this.numeroIteracoes > iteracao) {

			ArrayList<Individuo> novaPopulacao = new ArrayList<Individuo>();

			while (novaPopulacao.size() < populacao.size()) {

				// Selecinar cruzamento por Roleta
				Individuo pai1 = this.selecionaPai();
				Individuo pai2 = this.selecionaPai();

				// Cruzamento dos pais de acordo com a probabilidade e MutaÃ§Ã£o
				// de acordo com probabilidade
				if (random.nextDouble() <= this.probCruz) {
					// Cruzamento
					Individuo[] filhos = this.filhosCruzamento(pai1, pai2);

					// Mutacao de acordo com probabilidade
					this.mutacao(filhos[0]);
					this.mutacao(filhos[1]);

					novaPopulacao.add(filhos[0]);
					novaPopulacao.add(filhos[1]);

				}
			}

			this.populacao = null;
			this.populacao = (ArrayList<Individuo>) novaPopulacao.clone();
			novaPopulacao.clear();

			double fitnessPopulacao = 0;
			// Avaliar Nova Populacao
			for (Individuo individuo : this.populacao) {
				avalia(individuo);
				fitnessPopulacao += (double) individuo.getFitness();
			}

			// Ranqueia populacao
			this.rank();

			System.out.println("");
			System.out.println("Iteraçao: " + iteracao);
			double[] XY = this.converteBinarioEmReal(melhorIndividuo.getCromossomo());
			System.out.println(
					"Melhor Individuo: " + melhorIndividuo.getCromossomo() + ": " + XY[0] + " " + XY[1] + " : "+ melhorIndividuo.getFitness());

			this.insereNoGrafico(fitnessPopulacao, iteracao);

			/*
			 * if (this.melhorIndividuo.getFitness() == objetivo.length()) {
			 * insereNoGrafico(fitnessPopulacao, iteracao); if (iteracaoFinal.equals(""))
			 * iteracaoFinal = "" + iteracao; // break; }
			 */
			/*
			 * try { Thread.sleep(50);
			 * 
			 * } catch (InterruptedException e) { // TODO Auto-generated catch block
			 * e.printStackTrace(); }
			 */
			iteracao++;

		}

		this.salvarGrafico("" + iteracao);

	}

	private void insereNoGrafico(double fitnessPopulacao, int iteracao) {

		ds.addValue((fitnessPopulacao / this.populacao.size()), "Media de Fitness da População", "" + iteracao);
		ds.addValue(this.melhorIndividuo.getFitness(), "Fitness do Melhor Individuo", "" + iteracao);
	}

	// Gerar populaÃ§Ã£o inicial de numeros reais
	private ArrayList<Individuo> populacaoInicialReais() {

		ArrayList<Individuo> populacaoInicial = new ArrayList<Individuo>();

		DoubleStream dsX = new Random().doubles(-100, 100);
		double[] numerosX = dsX.limit(this.tamanhoPopulacao).toArray();
		
		DoubleStream dsY = new Random().doubles(-100, 100);
		double[] numerosY = dsY.limit(this.tamanhoPopulacao).toArray();

		for (int i = 0; i < numerosX.length; i++) {
			String cromossomo = converteRealEmBinario(numerosX[i]) + converteRealEmBinario(numerosY[i]);

			Individuo individuo = new Individuo(cromossomo);

			avalia(individuo);

			populacaoInicial.add(individuo);
		}

		return populacaoInicial;
	}

	// SeleÃ§Ã£o
	private Individuo selecionaPai() {

		int somatorio = 0;
		double probabilidades = 0.0;
		Individuo pai = null;

		for (Individuo individuo : this.populacao) {
			somatorio += individuo.getFitness();
		}

		// Roleta
		double ponteiro = random.nextDouble();

		for (Individuo individuo : this.populacao) {
			if (ponteiro <= ((double) individuo.getFitness() / (double) somatorio) + probabilidades) {
				pai = new Individuo(individuo.getFitness(), individuo.getCromossomo());
				break;
			} else {
				probabilidades += ((double) individuo.getFitness() / (double) somatorio);
			}
		}

		return pai;
	}

	// Cruzamento
	private Individuo[] filhosCruzamento(Individuo pai1, Individuo pai2) {

		Individuo filho1 = new Individuo();
		Individuo filho2 = new Individuo();

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

		Individuo[] filhos = { filho1, filho2 };

		return filhos;
	}

	// MutaÃ§Ã£o
	private void mutacao(Individuo filho) {

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
	private void avalia(Individuo individuo) {
		String cromossomo = individuo.getCromossomo();
		
		double[] valoresXY = converteBinarioEmReal(cromossomo);
		
		double pontuacao = ScafferF6(valoresXY[0], valoresXY[1]);
		
		individuo.setFitness(pontuacao);
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

	// Ranqueamento da populaÃ§Ã£o
	public void rank() {

		Collections.sort(this.populacao);

		// if (this.melhorIndividuo.getFitness() <
		// populacao.get(0).getFitness()){
		this.melhorIndividuo = new Individuo(populacao.get(0).getFitness(), populacao.get(0).getCromossomo());
		// }
	}

	public JPanel getPanel() {
		this.grafico = ChartFactory.createLineChart(this.tituloGrafico, "Iteração", "Fitness", ds,
				PlotOrientation.VERTICAL, true, true, false);
		return new ChartPanel(this.grafico);
	}

	private void salvarGrafico(String encerramento) {
		this.grafico = ChartFactory.createLineChart(
				this.tituloGrafico + "\nSolução encontrada na geração: " + this.iteracaoFinal, "Iteração", "Fitness",
				ds, PlotOrientation.VERTICAL, true, true, false);
		OutputStream arq;
		try {
			arq = new FileOutputStream("AlgoritmoGenetic_2\\" + this.nomeGrafico + ".png");
			ChartUtilities.writeChartAsPNG(arq, grafico, 680, 480);
			arq.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		
		String binY = String.copyValueOf(binario.toCharArray(), (binario.length() / 2), (binario.length() / 2)-1);
		
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
		int[] populacoes = { 50 };

		for (int j = 0; j < populacoes.length; j++) {
			for (int j2 = 0; j2 < mutacoes.length; j2++) {
				String titulo = "Evolução do AG com parametros de Mut: " + mutacoes[j2] + ", População: "
						+ populacoes[j];
				String nomeGrafico = "Mut-" + mutacoes[j2] + "_População-" + populacoes[j];
				// AG_ExtF6 ag = new AG_ExtF6(populacoes[j], 500,palavras[i], .75, mutacoes[j2],
				// titulo, nomeGrafico, 28, 6);
				AG_ExtF6 ag = new AG_ExtF6(populacoes[j], 500, .75, mutacoes[j2], titulo, nomeGrafico, 28, 6);
				Thread t = new Thread(ag);
				// Cria a linha de execução
				t.start(); // Ativa a thread } } }

			}
		}

		// 28 bits para representar os numero -100,100 com 6 casas decimais quando
		// converter para real, considerar o piso igual a -100

		/*
		 * AG ag = new AG(10, 1000, "1010101010101010", 1.0, 0.01, String titulo, String
		 * nomeGrafico);
		 * 
		 * JFrame frame = new JFrame("Gráfico de Linha do Algoritmo Genético");
		 * frame.add(ag.getPanel());
		 * 
		 * frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); frame.pack();
		 * frame.setSize(1024, 480); frame.setVisible(true);
		 * 
		 * Thread t = new Thread(ag); //Cria a linha de execução t.start(); //Ativa a
		 * thread
		 * 
		 */
	}

}

class Individuo implements Comparable<Individuo> {

	private double fitness = 0;
	private String cromossomo;

	public Individuo() {
	}

	public Individuo(String cromossomo) {
		this.cromossomo = cromossomo;
	}

	public Individuo(double fitness, String cromossomo) {
		super();
		this.fitness = fitness;
		this.cromossomo = cromossomo;
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
	public int compareTo(Individuo outroIndividuo) {
		// TODO Auto-generated method stub
		if (this.fitness < outroIndividuo.getFitness()) {
			return -1;
		}
		if (this.fitness > outroIndividuo.getFitness()) {
			return 1;
		}
		return 0;
	}

}
