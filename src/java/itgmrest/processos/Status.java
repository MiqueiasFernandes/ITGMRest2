/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package itgmrest.processos;

import java.io.StringReader;
import javax.json.Json;
import static javax.json.Json.createReader;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

/**
 *
 * @author mfernandes
 */
public class Status {

    private boolean isAlive, isSuspenso;
    private String base, pid, pidJava, cpu, memoria, disco, error;
    private String memoriaLimit;
    private String cpuLimit;
    private String discoLimit;
    private Contexto contexto;
    private JsonObject readObject;

    public Status(String base, Contexto contexto, boolean isSuspenso) {
        this.base = base;
        this.contexto = contexto;
        this.isSuspenso = isSuspenso;
        this.cpuLimit = contexto.getCpuLimite();
        this.memoriaLimit = contexto.getMemoriaLimite();
        this.discoLimit = contexto.getDiscoLimite();
        try {
            readObject = createReader(new StringReader(base)).readObject();
        } catch (Exception ex) {
            error = ex.toString();
        }
        try {
            isAlive = readObject.getBoolean("isAlive");
        } catch (Exception ex) {
            error = ex.toString();
        }
        try {
            pid = readObject.getString("pid");
        } catch (Exception ex) {
            error = ex.toString();
        }
        try {
            pidJava = readObject.getString("pidJava");
        } catch (Exception ex) {
            error = ex.toString();
        }
        try {
            cpu = readObject.getString("cpu");
        } catch (Exception ex) {
            error = ex.toString();
        }
        try {
            memoria = readObject.getString("memoria");
        } catch (Exception ex) {
            error = ex.toString();
        }
        try {
            disco = readObject.getString("disco");
        } catch (Exception ex) {
            error = ex.toString();
        }

    }

    public String getJson() {
        try {
            JsonObjectBuilder builder = Json.createObjectBuilder()
                    .add("isSuspenso", isSuspenso)
                    .add("error", error != null ? error : "ok")
                    .add("memoriaLimit", memoriaLimit)
                    .add("cpuLimit", cpuLimit)
                    .add("discoLimit", discoLimit)
                    .add("token", contexto.getToken())
                    .add("diretorio", contexto.getDiretorioPath());

            readObject.forEach((String key, JsonValue value) -> {
                builder.add(key, value);
            });

            return builder.build().toString();

        } catch (Exception ex) {
            error = ex.toString();
        }
        return "{\"error\":\"" + error + "\"}";
    }

    public boolean isIsAlive() {
        return isAlive;
    }

    public boolean isIsSuspenso() {
        return isSuspenso;
    }

    public String getBase() {
        return base;
    }

    public String getPid() {
        return pid;
    }

    public String getPidJava() {
        return pidJava;
    }

    public String getCpu() {
        return cpu;
    }

    public String getMemoria() {
        return memoria;
    }

    public String getDisco() {
        return disco;
    }

    public String getError() {
        return error;
    }

    public Contexto getContexto() {
        return contexto;
    }

//    public void setMemoryInicial(String size) {
//        try {
//            int mem = Integer.parseInt(memoria);
//            int old = Integer.parseInt(size);
//            if (mem > old) {
//                mem -= old;
//                memoria = String.valueOf(mem);
//            }
//        } catch (Exception ex) {
//            error = ex.toString();
//        }
//    }
//    public void setCpuLimit(String cpuLimit) {
//        this.cpuLimit = cpuLimit;
//    }
    public String getDiscoLimit() {
        return discoLimit;
    }

    public String getMemoriaLimit() {
        return memoriaLimit;
    }

    public String getCpuLimit() {
        return cpuLimit;
    }

    public JsonObject getReadObject() {
        return readObject;
    }

    @Override
    public String toString() {
        String saida = "Status{"
                + "isSuspenso=" + isSuspenso
                + ", error=" + error
                + ", memoriaLimit=" + memoriaLimit
                + ", cpuLimit=" + cpuLimit
                + ", discoLimit=" + discoLimit;

        saida = readObject.entrySet().stream().map(
                (entry) -> ", " + entry.getKey() + "=" + entry.getValue().toString())
                .reduce(saida, String::concat);

        return saida + '}';
    }

}
