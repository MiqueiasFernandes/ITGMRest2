/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package itgmrest.webservice;

import itgmrest.MainSingleton;
import itgmrest.ScriptBash;
import itgmrest.processos.AbstractProcesso;
import itgmrest.processos.BatchProcesso;
import itgmrest.processos.Contexto;
import itgmrest.processos.LiveProcesso;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

/**
 * REST Web Service
 *
 * @author mfernandes
 */
@Path("jriaccess")
public class ITGMRestResource {

    @Context
    private UriInfo context;

    /**
     * Retrieves representation of an instance of
     * itgmrest.webservice.ITGMRestResource
     *
     * @return json with status of process
     */
    @GET
    @Path("process/")
    @Produces(MediaType.APPLICATION_JSON)
    public String getProcess() {
        getMainSingleton().info("@GET/PROCESSOS : obtendo processos");
        try {

            StringBuilder stringBuilder = new StringBuilder();

            for (Iterator<AbstractProcesso> iterator = getMainSingleton().getIterator(); iterator.hasNext();) {
                AbstractProcesso next = iterator.next();
                stringBuilder
                        .append(stringBuilder.length() > 0 ? "," : "")
                        .append(next.getStatus().getJson());
            }

            return "{\"process\":[" + stringBuilder.toString() + "]}";

        } catch (Exception ex) {
            return "{\"error\":\"" + ex
                    .toString()
                    .replaceAll("\\s", " ")
                    .replaceAll("\"", "'") + "\"}";
        }

    }

    @GET
    @Path("status/{token}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getStatus(@PathParam("token") String token) {
        getMainSingleton().info("@GET/STATUS : obtendo status do processo " + token);
        try {
            AbstractProcesso processo = getProcess(token, "@GET/STATUS");

            if (processo != null) {
                return processo.getStatus().getJson();
            } else {
                return "{\"error\":\"processo não encontrado\"}";
            }
        } catch (Exception ex) {
            return "{\"error\":\"" + ex
                    .toString()
                    .replaceAll("\\s", " ")
                    .replaceAll("\"", "'") + "\"}";
        }

    }

    @GET
    @Path("list/{usuario}/{projeto}/{cenario}/{diretorio}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getListOfFiles() {
        getMainSingleton().info("@GET/LIST : a listar arquivos de diretorio");
        try {
            MultivaluedMap<String, String> pathParameters = context.getPathParameters();
            String local = MainSingleton.DIRETORIO
                    + pathParameters.getFirst("usuario") + File.separator
                    + (!"*".equals(pathParameters.getFirst("projeto"))
                    ? pathParameters.getFirst("projeto") + File.separator : "")
                    + (!"*".equals(pathParameters.getFirst("cenario"))
                    ? pathParameters.getFirst("cenario") + File.separator : "")
                    + (!"*".equals(pathParameters.getFirst("diretorio"))
                    ? pathParameters.getFirst("diretorio") + File.separator : "");
            getMainSingleton().debug("@GET/LIST : a listar arquivos de diretorio: " + local, getClass(), 130);

            return list(new File(local));
        } catch (Exception ex) {
            return "error:" + ex;
        }
    }

    @GET
    @Path("file/{usuario}/{projeto}/{cenario}/{diretorio}/{file}")
    @Produces(MediaType.TEXT_PLAIN)
    public String publicFile(@PathParam("file") String file) {
        getMainSingleton().info("@GET/PUBLIC : a publicar arquivo " + file);
        try {
            MultivaluedMap<String, String> pathParameters = context.getPathParameters();
            String local = MainSingleton.DIRETORIO
                    + pathParameters.getFirst("usuario") + File.separator
                    + (!"*".equals(pathParameters.getFirst("projeto"))
                    ? pathParameters.getFirst("projeto") + File.separator : "")
                    + (!"*".equals(pathParameters.getFirst("cenario"))
                    ? pathParameters.getFirst("cenario") + File.separator : "")
                    + (!"*".equals(pathParameters.getFirst("diretorio"))
                    ? pathParameters.getFirst("diretorio") + File.separator : "");
            if (context.getQueryParameters().getFirst("subdiretorio") != null
                    && !context.getQueryParameters().getFirst("subdiretorio").isEmpty()) {
                local += context.getQueryParameters().getFirst("subdiretorio");
            }

            local += file;
            String token = getMainSingleton().nextTokenFile();
            String nome;
            File publico = new File(MainSingleton.WWW + (nome = (token + "." + local.replaceAll("^.*\\.", ""))));
            Files.copy(new File(local).toPath(), publico.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return nome;
        } catch (Exception ex) {
            return "error:" + ex;
        }
    }

    public String list(File file) {

        String files = file.getAbsolutePath().replace(MainSingleton.DIRETORIO, "");

        if (file.isDirectory()) {
            files += "/";
            for (File f : file.listFiles()) {
                files += "," + list(f);
            }

        }
        return files;
    }

    /**
     * PUT method for updating or creating an instance of ITGMRestResource
     *
     * @param content representation for the resource
     * @return token of process
     */
    @PUT
    @Path("{usuario}/{projeto}/{cenario}/{diretorio}")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public String putProcess(String content) {
        getMainSingleton().info("@PUT/PROCESS");
        Contexto contexto;
        try {
            contexto = new Contexto(context.getPathParameters(), context.getQueryParameters(), content);
        } catch (Exception ex) {
            getMainSingleton().error("impossivel criar contexto de " + context + " get: " + ex, getClass(), 50);
            return null;
        }

        AbstractProcesso processo
                = contexto.isIsLive() ? new LiveProcesso(contexto) : new BatchProcesso(contexto);

        getMainSingleton().info("novo processo: " + processo);

        if (processo.processar()) {
            getMainSingleton().addProcesso(processo);
        } else {
            return null;
        }
        return processo.getContexto().getToken();
    }

    @PUT
    @Path("update")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean putUpdate(String token) {
        getMainSingleton().info("@PUT/UPDATE " + token);
        Contexto contexto;
        try {
            contexto = new Contexto(token, context.getQueryParameters());
        } catch (Exception ex) {
            getMainSingleton().error("impossivel criar contexto de " + context + " EX: " + ex, getClass(), 50);
            return false;
        }

        try {
            AbstractProcesso processo = getProcess(token, "@PUT/UPDATE");

            if (processo != null) {
                return processo.setContexto(contexto);
            } else {
                getMainSingleton().warning("processo não encontrado: " + token);
            }
        } catch (Exception ex) {
            getMainSingleton().error("impossivel efetuar update: " + ex, this.getClass(), 119);
        }

        return false;
    }

    @PUT
    @Path("suspend")
    @Consumes(MediaType.TEXT_PLAIN)
    public void suspendProcess(String token) {
        getMainSingleton().info("@PUT/SUSPEND : a suspender processo");
        AbstractProcesso processo = getProcess(token, "@PUT/SUSPEND");
        if (processo != null) {
            processo.suspend();
        }
    }

    @PUT
    @Path("resume")
    @Consumes(MediaType.TEXT_PLAIN)
    public void resumeProcess(String token) {
        getMainSingleton().info("@PUT/RESUME");
        AbstractProcesso processo = getProcess(token, "@PUT/RESUME");
        if (processo != null) {
            processo.resume();
        }
    }

    @PUT
    @Path("stop")
    @Consumes(MediaType.TEXT_PLAIN)
    public void stopProcess(String token) {
        getMainSingleton().info("@PUT/STOP");
        AbstractProcesso processo = getProcess(token, "@PUT/STOP");
        if (processo != null) {
            processo.stop();
        }
    }

    @DELETE
    @Path("process/{token}")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean deleteProcess(@PathParam("token") String token) {
        getMainSingleton().info("@DELETE/PROCESS");
        try {
            AbstractProcesso processo = getProcess(token, "@DELETE");

            if (processo != null) {
                return processo.encerrarSessao("comando @DELETE");
            }
        } catch (Exception ex) {
            getMainSingleton().error("impossivel deletar processo: " + ex, this.getClass(), 119);
        }

        return false;
    }

    @DELETE
    @Path("usuario/{usuario}")
    public void deleteUsuario(
            @PathParam("usuario") String usuario) {
        getMainSingleton().info("@DELETE/USUARIO " + usuario);
        ScriptBash.updatePID("0", "0", "0", "sudo rm "
                + MainSingleton.DIRETORIO
                + usuario + File.separator
                + " -r -f");
    }

    @DELETE
    @Path("projeto/{usuario}/{projeto}")
    public void deleteProjeto(
            @PathParam("usuario") String usuario,
            @PathParam("projeto") String projeto) {
        getMainSingleton().info("@DELETE/PROJETO " + projeto);
        ScriptBash.updatePID("0", "0", "0", "sudo rm "
                + MainSingleton.DIRETORIO
                + usuario + File.separator
                + projeto + File.separator
                + " -r -f");
    }

    @DELETE
    @Path("cenario/{usuario}/{projeto}/{cenario}")
    public void deleteCenario(
            @PathParam("usuario") String usuario,
            @PathParam("projeto") String projeto,
            @PathParam("cenario") String cenario) {
        getMainSingleton().info("@DELETE/CENARIO " + cenario);
        ScriptBash.updatePID("0", "0", "0", "sudo rm "
                + MainSingleton.DIRETORIO
                + usuario + File.separator
                + projeto + File.separator
                + cenario + File.separator
                + " -r -f");
    }

    @DELETE
    @Path("diretorio/{usuario}/{projeto}/{cenario}/{diretorio}")
    public void deleteDiretorio(
            @PathParam("usuario") String usuario,
            @PathParam("projeto") String projeto,
            @PathParam("cenario") String cenario,
            @PathParam("diretorio") String diretorio) {
        getMainSingleton().info("@DELETE/DIRETORIO " + diretorio);
        ScriptBash.updatePID("0", "0", "0", "sudo rm "
                + MainSingleton.DIRETORIO
                + usuario + File.separator
                + projeto + File.separator
                + cenario + File.separator
                + diretorio + File.separator
                + " -r -f");
    }

    @DELETE
    @Path("file/{usuario}/{projeto}/{cenario}/{diretorio}/{file}")
    @Produces(MediaType.TEXT_PLAIN)
    public void deleteFile(
            @PathParam("file") String file) {
        getMainSingleton().info("@DELETE/FILE " + file);
        try {
            MultivaluedMap<String, String> pathParameters = context.getPathParameters();
            String arquivo = MainSingleton.DIRETORIO
                    + pathParameters.getFirst("usuario") + File.separator
                    + (!"*".equals(pathParameters.getFirst("projeto"))
                    ? pathParameters.getFirst("projeto") + File.separator : "")
                    + (!"*".equals(pathParameters.getFirst("cenario"))
                    ? pathParameters.getFirst("cenario") + File.separator : "")
                    + (!"*".equals(pathParameters.getFirst("diretorio"))
                    ? pathParameters.getFirst("diretorio") + File.separator : "");

            if (context.getQueryParameters().getFirst("subdiretorio") != null
                    && !context.getQueryParameters().getFirst("subdiretorio").isEmpty()) {
                arquivo += context.getQueryParameters().getFirst("subdiretorio");
            }

            arquivo += file;
            if (context.getPath().endsWith("/")) {
                arquivo += "/";
            }
            ScriptBash.updatePID("0", "0", "0", "sudo rm " + arquivo + " -r -f");
        } catch (Exception ex) {
            getMainSingleton().error("impossivel deletar arquivo " + file + ": " + ex, this.getClass(), 324);
        }
    }

    @POST
    @Path("{usuario}/{projeto}/{cenario}/{diretorio}/{file}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public boolean postFileContent(String fileContent) {
        getMainSingleton().info("@POST/FILE recebendo arquivo");
        try {
            MultivaluedMap<String, String> pathParameters = context.getPathParameters();
            String file = MainSingleton.DIRETORIO
                    + pathParameters.getFirst("usuario") + File.separator
                    + (!"*".equals(pathParameters.getFirst("projeto"))
                    ? pathParameters.getFirst("projeto") + File.separator : "")
                    + (!"*".equals(pathParameters.getFirst("cenario"))
                    ? pathParameters.getFirst("cenario") + File.separator : "")
                    + (!"*".equals(pathParameters.getFirst("diretorio"))
                    ? pathParameters.getFirst("diretorio") + File.separator : "");

            if (context.getQueryParameters().getFirst("subdiretorio") != null
                    && !context.getQueryParameters().getFirst("subdiretorio").isEmpty()) {
                file += context.getQueryParameters().getFirst("subdiretorio");
            }

            file += pathParameters.getFirst("file");
            if (context.getPath().endsWith("/")) {
                file += "/";
            } else {
                new File(file.substring(0, file.lastIndexOf("/") + 1)).mkdirs();
            }

            String conteudo = file.endsWith("/") ? null : URLDecoder.decode(fileContent, "UTF-8");
            getMainSingleton().debug("arquivo: " + file + " content: "
                    + (conteudo == null ? "(diretorio)"
                            : conteudo.substring(0, Integer.min(conteudo.length(), 10))) + "...", getClass(), 288);
            File f = new File(file);
            if (file.endsWith("/")) {
                return f.mkdirs();
            } else {
                f.createNewFile();
                if (conteudo != null && !conteudo.isEmpty() && conteudo.length() > 0) {
                    FileWriter fw = new FileWriter(f, Boolean.valueOf(context.getQueryParameters().getFirst("append")));
                    fw.write(conteudo);
                    fw.close();
                }
                return true;
            }
        } catch (Exception ex) {
            getMainSingleton().error("impossivel receber arquivo: " + ex, getClass(), 309);
            return false;
        }
    }

    @POST
    @Path("{usuario}/{projeto}/{cenario}/{diretorio}/{file}")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public boolean sendBinary(InputStream fileInputStream) {
        getMainSingleton().info("@POST/FILE/BINARY recebendo arquivo");
        try {
            MultivaluedMap<String, String> pathParameters = context.getPathParameters();
            String file = MainSingleton.DIRETORIO
                    + pathParameters.getFirst("usuario") + File.separator
                    + (!"*".equals(pathParameters.getFirst("projeto"))
                    ? pathParameters.getFirst("projeto") + File.separator : "")
                    + (!"*".equals(pathParameters.getFirst("cenario"))
                    ? pathParameters.getFirst("cenario") + File.separator : "")
                    + (!"*".equals(pathParameters.getFirst("diretorio"))
                    ? pathParameters.getFirst("diretorio") + File.separator : "");

            if (context.getQueryParameters().getFirst("subdiretorio") != null
                    && !context.getQueryParameters().getFirst("subdiretorio").isEmpty()) {
                file += context.getQueryParameters().getFirst("subdiretorio");
            }

            file += pathParameters.getFirst("file");
            if (context.getPath().endsWith("/")) {
                file += "/";
            } else {
                new File(file.substring(0, file.lastIndexOf("/") + 1)).mkdirs();
            }

            getMainSingleton().debug("arquivo: " + file + " content: " + fileInputStream.toString() + "...", getClass(), 288);
            File f = new File(file);
            if (file.endsWith("/")) {
                return false;
            } else {
                f.createNewFile();
                if (fileInputStream.available() > 0) {
                    Files.copy(fileInputStream, f.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                return true;
            }
        } catch (Exception ex) {
            getMainSingleton().error("impossivel receber arquivo binario: " + ex, getClass(), 309);
            return false;
        }
    }

    public MainSingleton getMainSingleton() {
        return MainSingleton.getInstance();
    }

    public AbstractProcesso getProcess(String token, String metodo) {
        try {
            AbstractProcesso processo = getMainSingleton().getProcesso(token);

            if (processo != null) {
                return processo;
            } else {
                getMainSingleton().warning("processo não encontrado: " + token + " para " + metodo);
            }
        } catch (Exception ex) {
            getMainSingleton().error("impossivel efetuar " + metodo + ": " + ex, this.getClass(), 200);
        }
        return null;
    }

}
