/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package itgmrest.processos;

import itgmrest.MainSingleton;
import java.io.File;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.Arrays;
import javax.ws.rs.core.MultivaluedMap;

/**
 *
 * @author mfernandes
 */
public class Contexto {

    private final MultivaluedMap<String, String> pathParameters;
    private final MultivaluedMap<String, String> queryParameters;
    private final String usuario;
    private final String projeto;
    private final String cenario;
    private final String diretorio;
    private final String diretorioPath;
    private final File diretorioFile;
    private final String[] parametros;
    private final String memoriaLimite;
    private final String cpuLimite;
    private final String discoLimite;
    private final boolean isLive, isBatch;
    private final boolean salvar;
    private final boolean valido;
    private final String conteudo;
    private final String conteudoDecoded;
    private final String token;

    public Contexto(String usuario, 
            String projeto, 
            String cenario, 
            String diretorio, 
            String[] parametros, 
            boolean isLive, 
            boolean salvar, 
            String conteudo, 
            String token) {
        this.usuario = usuario;
        this.projeto = projeto;
        this.cenario = cenario;
        this.diretorio = diretorio;
        this.parametros = parametros;
        this.memoriaLimite = "500";
        this.cpuLimite = "500";
        this.discoLimite = "500";
        this.isLive = isLive;
        this.isBatch = !isLive;
        this.salvar = salvar;
        this.valido = true;
        this.conteudo = conteudo;
        this.conteudoDecoded = conteudo;
        this.token = token;
        diretorioPath = MainSingleton.DIRETORIO
                + usuario
                + File.separator + projeto
                + File.separator + cenario
                + File.separator + diretorio + File.separator;
        diretorioFile = new File(diretorioPath);
        if (!Files.exists(diretorioFile.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            diretorioFile.mkdirs();
        }
       this.pathParameters = null;
       this.queryParameters = null;
    }

    
    
    
    
    public Contexto(MultivaluedMap<String, String> pathParameters,
            MultivaluedMap<String, String> queryParameters,
            String conteudo) throws Exception {
        this.valido = (pathParameters != null
                && pathParameters.containsKey("usuario")
                && pathParameters.containsKey("projeto")
                && pathParameters.containsKey("cenario")
                && pathParameters.containsKey("diretorio")
                && pathParameters.getFirst("usuario") != null && !pathParameters.getFirst("usuario").isEmpty()
                && pathParameters.getFirst("projeto") != null && !pathParameters.getFirst("projeto").isEmpty()
                && pathParameters.getFirst("cenario") != null && !pathParameters.getFirst("cenario").isEmpty()
                && pathParameters.getFirst("diretorio") != null && !pathParameters.getFirst("diretorio").isEmpty()
                && queryParameters != null
                && queryParameters.containsKey("parametros") && queryParameters.get("parametros").size() > 2
                && queryParameters.containsKey("memoria") && !queryParameters.getFirst("memoria").isEmpty()
                && queryParameters.containsKey("cpu") && !queryParameters.getFirst("cpu").isEmpty()
                && queryParameters.containsKey("disco") && !queryParameters.getFirst("disco").isEmpty()
                && queryParameters.containsKey("salvar") && !queryParameters.getFirst("salvar").isEmpty()
                && (("BATCH".equals(queryParameters.get("parametros").get(0)) && conteudo != null)
                || "LIVE".equals(queryParameters.get("parametros").get(0))));
        if (!valido) {
            throw new Exception("argumentos invalidos no contexto");
        }
        this.pathParameters = pathParameters;
        this.queryParameters = queryParameters;
        this.conteudo = conteudo;
        this.token = MainSingleton.getInstance().nextToken();
        usuario = pathParameters.getFirst("usuario");
        projeto = pathParameters.getFirst("projeto");
        cenario = pathParameters.getFirst("cenario");
        diretorio = pathParameters.getFirst("diretorio");
        parametros = queryParameters.get("parametros").toArray(new String[]{});
        memoriaLimite = queryParameters.getFirst("memoria");
        cpuLimite = queryParameters.getFirst("cpu");
        discoLimite = queryParameters.getFirst("disco");
        isBatch = (false == (isLive = "LIVE".equals(parametros[0].toUpperCase())));
        salvar = !isLive || Boolean.valueOf(queryParameters.getFirst("salvar"));
        diretorioPath = MainSingleton.DIRETORIO
                + usuario
                + File.separator + projeto
                + File.separator + cenario
                + File.separator + diretorio + File.separator;
        diretorioFile = new File(diretorioPath);
        if (!Files.exists(diretorioFile.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            diretorioFile.mkdirs();
        }
        if (isBatch) {
            conteudoDecoded = URLDecoder.decode(conteudo, "UTF-8");
        } else {
            conteudoDecoded = "";
        }
    }

    public Contexto(String token, MultivaluedMap<String, String> queryParameters) throws Exception {
        this.token = token;

        AbstractProcesso processo = getMainSingleton().getProcesso(token);

        if (processo == null) {
            valido = false;
            throw new Exception("impossivel obter processo com token: " + token);
        }
        Contexto contexto = processo.getContexto();

        if (contexto == null || !contexto.isValido()) {
            throw new Exception("impossivel reconstruir contexto a partir de invalido.");
        }

        if (!(queryParameters != null
                && queryParameters.containsKey("memoria") && !queryParameters.getFirst("memoria").isEmpty()
                && queryParameters.containsKey("cpu") && !queryParameters.getFirst("cpu").isEmpty()
                && queryParameters.containsKey("disco") && !queryParameters.getFirst("disco").isEmpty()
                && queryParameters.containsKey("salvar") && !queryParameters.getFirst("salvar").isEmpty())) {
            valido = false;
            throw new Exception("queryParameters invalido.");
        }

        this.pathParameters = contexto.getPathParameters();
        this.queryParameters = queryParameters;
        this.conteudo = contexto.getConteudo();
        usuario = contexto.getUsuario();
        projeto = contexto.getProjeto();
        cenario = contexto.getCenario();
        diretorio = contexto.getDiretorio();
        parametros = contexto.getParametros();
        memoriaLimite = queryParameters.getFirst("memoria");
        cpuLimite = queryParameters.getFirst("cpu");
        discoLimite = queryParameters.getFirst("disco");
        salvar = !contexto.isLive || Boolean.valueOf(queryParameters.getFirst("salvar"));
        isLive = contexto.isIsLive();
        isBatch = contexto.isBatch;
        diretorioPath = contexto.getDiretorioPath();
        diretorioFile = contexto.getDiretorioFile();
        conteudoDecoded = contexto.getConteudoDecoded();
        this.valido = true;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getProjeto() {
        return projeto;
    }

    public String getCenario() {
        return cenario;
    }

    public String getDiretorio() {
        return diretorio;
    }

    public String getDiretorioPath() {
        return diretorioPath;
    }

    public String[] getParametros() {
        return parametros;
    }

    public String getMemoriaLimite() {
        return memoriaLimite;
    }

    public String getDiscoLimite() {
        return discoLimite;
    }

    public String getCpuLimite() {
        return cpuLimite;
    }

    public boolean isIsLive() {
        return isLive;
    }

    public boolean isSalvar() {
        return salvar;
    }

    public boolean isValido() {
        return valido;
    }

    public MultivaluedMap<String, String> getPathParameters() {
        return pathParameters;
    }

    public MultivaluedMap<String, String> getQueryParameters() {
        return queryParameters;
    }

    public String getConteudo() {
        return conteudo;
    }

    public String getConteudoDecoded() {
        return conteudoDecoded;
    }

    public File getDiretorioFile() {
        return diretorioFile;
    }

    public String getToken() {
        return token;
    }

    @Override
    public String toString() {
        return "Contexto{"
                + "usuario=" + usuario
                + ", projeto=" + projeto
                + ", cenario=" + cenario
                + ", diretorio=" + diretorio
                + ", diretorioPath=" + diretorioPath
                + ", parametros=" + Arrays.toString(parametros)
                + ", memoriaLimite=" + memoriaLimite
                + ", cpuLimite=" + cpuLimite
                + (isLive ? (", isLive=" + isLive)
                        : (", isBatch=" + isBatch + ", conteudo=" + conteudo))
                + ", salvar=" + salvar + '}';
    }

    public static MainSingleton getMainSingleton() {
        return MainSingleton.getInstance();
    }

}
