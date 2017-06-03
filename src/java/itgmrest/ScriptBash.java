/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package itgmrest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author mfernandes
 */
public class ScriptBash {

    private static int atualizacao = 1;

    private static final String pidsFile = "pidLimits";

    public static final String prefixoDoGrupo = "itgm";

    public static final String pathTemp = "pidsTemp/";

    public static final String timeToReloadFile = "0.5";

    public static void saveFileScript(MainSingleton singleton) {
        File fs = new File(MainSingleton.DIRETORIO);
        singleton.debug("buscando por script file em " + fs.getAbsolutePath(), ScriptBash.class, 150);
        for (String fname : fs.list()) {
            if (MainSingleton.BASH_SCRIPT.equals(fname)) {
                return;
            }
        }
        File script = new File(MainSingleton.DIRETORIO + MainSingleton.BASH_SCRIPT);
        singleton.debug("criando arquivo script em " + script.getAbsolutePath(),
                ScriptBash.class, 157);
        try {
            script.createNewFile();
        } catch (IOException ex) {
            singleton.error("impossivel criar o arquivo " + MainSingleton.BASH_SCRIPT
                    + " ex: " + ex, ScriptBash.class, 163);
        }
        singleton.debug("escrevendo dados no arquivo de script", ScriptBash.class, 163);
        try (FileWriter fw = new FileWriter(script)) {
            fw.write(fileContent);
            fw.close();
        } catch (Exception ex) {
            singleton.error("impossivel escrever no arquivo "
                    + MainSingleton.BASH_SCRIPT + " ex: " + ex, ScriptBash.class, 170);
        }
        File dir = new File(MainSingleton.DIRETORIO + pathTemp);
        singleton.debug("criando diretorio de arquivos temporarios de pids em "
                + dir.getAbsolutePath(), ScriptBash.class, 173);
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                singleton.error("impossivel criar diretorio de pids file temporario em "
                        + dir.getAbsolutePath(), ScriptBash.class, 175);
            } else {
                singleton.info("diretorio " + dir.getAbsolutePath() + " criado com sucesso!");
            }
        } else {
            singleton.warning("a utilizar diretorio de pid temporario em "
                    + dir.getAbsolutePath());
        }

        singleton.warning("O ARQUIVO BASH CRIADO EM: " + script.getAbsolutePath()
                + "\nDEVE SER EXECUTADO IMEDIATAMENTE.\nUSE: chmod +x "
                + script.getAbsolutePath() + " ; " + script.getAbsolutePath());
        singleton.debug("tarefa saveFileScript() efetuada com sucesso.", ScriptBash.class, 188);
    }

    public static void putPID(String pid, String cpu, String mem) {
        File pids = new File(MainSingleton.DIRETORIO + pathTemp + pidsFile);
        getBase().debug("escrevendo em arquivo de pids " + pid + ":"
                + cpu + ":"
                + mem, ScriptBash.class, 195);
        try (FileWriter fw = new FileWriter(pids, true)) {
            fw.write(pid + ":"
                    + cpu + ":"
                    + mem + System.lineSeparator());
        } catch (IOException ex) {
            getBase().error("impossivel escrever '"
                    + pid + ":"
                    + cpu + ":"
                    + mem + "' no arquivo " + pids.getAbsolutePath() + " ex: " + ex,
                    ScriptBash.class, 205);
        }
        getBase().info("configuração de recursos do PID: " + pid + " atribuida com sucesso!");
    }

    public static void updatePID(String pid, String cpu, String mem, String commando) {
        File pids = new File(MainSingleton.DIRETORIO + pathTemp + pidsFile);
        getBase().debug("atualizando: PID: " + pid + " CPU: " + cpu + " MEM: " + mem + " COM: " + commando, ScriptBash.class, 95);
        String id = atualizacao++ + pid, tmp = "";
        if (pid == null || pid.isEmpty() || pid.matches(".*\\D.*")) {
            getBase().error("não é possivel atualizar pid não numerico: " + pid, ScriptBash.class, 99);
            return;
        }
        for (char c : id.toCharArray()) {
            tmp += (char) (int) (Integer.parseInt(c + "") + 100);
        }

        String text = tmp + ":" + prefixoDoGrupo + pid + ":" + cpu + ":" + mem + ":" + commando;

        getBase().debug("escrevendo atualizacao em arquivo de pids " + text, ScriptBash.class, 95);
        try (FileWriter fw = new FileWriter(pids, true)) {
            fw.write(text + System.lineSeparator());
        } catch (IOException ex) {
            getBase().error("impossivel escrever atualização '"
                    + text + "' no arquivo " + pids.getAbsolutePath() + " ex: " + ex,
                    ScriptBash.class, 105);
        }
        getBase().info("atualização de recursos do PID: " + pid + " atribuida com sucesso!");
    }

    public static void kill(String pid) {
        File pids = new File(MainSingleton.DIRETORIO + pathTemp + pidsFile);
        getBase().debug("escrevendo em arquivo de pids kill" + pid, ScriptBash.class, 195);
        try (FileWriter fw = new FileWriter(pids, true)) {
            fw.write("kill" + pid + System.lineSeparator());
        } catch (IOException ex) {
            getBase().error("impossivel escrever kill" + pid
                    + " no arquivo " + pids.getAbsolutePath() + " ex: " + ex,
                    ScriptBash.class, 205);
        }
        getBase().info("kill PID: " + pid + " enviado com sucesso!");
    }

    public static boolean consultarPID(String pid) {
        getBase().debug("a consultar existencia de arquivo: "
                + MainSingleton.DIRETORIO + pathTemp + prefixoDoGrupo + pid,
                ScriptBash.class, 213);
        boolean exists = new File(MainSingleton.DIRETORIO + pathTemp + prefixoDoGrupo + pid).exists();
        if (exists) {
            getBase().debug("o arquivo do PID " + pid + " exite!", ScriptBash.class, 225);
        } else {
            getBase().debug("o arquivo do PID " + pid + " não exite!", ScriptBash.class, 227);
        }
        return exists;
    }

    public static MainSingleton getBase() {
        return MainSingleton.getInstance();
    }

    private static final String fileContent = "#!/bin/bash\n"
            + "# Autor: Miquéias Fernandes - 14/03/2017 v1 S.O. Fedora25\n"
            + "# Script para automatização de gerenciamento de cgroups\n"
            + "\n"
            + "\n"
            + "pidArquivo=\"" + MainSingleton.DIRETORIO + pathTemp + pidsFile + "\"\n"
            + "cgPath=/sys/fs/cgroup/\n"
            + "cpuPath=\"cpu/\"\n"
            + "memPath=\"memory/\"\n"
            + "memSize=\"m\"\n"
            + "\n"
            + "if [[ $EUID -ne 0 ]]; then\n"
            + "	sudo echo \"Por favor, execute este programa como administrador\"\n"
            + "fi\n"
            + "\n"
            + "if [ ! -e \"$pidArquivo\" ] ; then\n"
            + "	echo \"criando arquivo $pidArquivo\"\n"
            + "	touch \"$pidArquivo\"\n"
            + "	sudo chmod 777 \"$pidArquivo\"\n"
            + "	sudo echo \"TYPE ON FORM PID:CPU:MEM\" > \"$pidArquivo\"\n"
            + "fi\n"
            + "\n"
            + "sudo echo \"INSIRA LINHA POR LINHA OS LIMITES NA FORMA PID:CPU:MEM NO ARQUIVO $pidArquivo ONDE:\"\n"
            + "sudo echo \"PID numero indentificador do processo\"\n"
            + "sudo echo \"CPU numero entre [1 A (NUCLEOS DO CPU)X100] ou [0.1 A 0.999]\"\n"
            + "sudo echo \"MEM tamanho da memoria em MegaBytes\"\n"
            + "declare -a pids\n"
            + "declare -a grupos\n"
            + "comando=\"cgcreate\"\n"
            + "if which -a \"$comando\" ; then\n"
            + "	echo \"Parece que está tudo certo com libcgroup-tool\"\n"
            + "time=0\n"
            + "	while true; do \n"
            + "		(( time++))\n"
            + "         if (( $time > 5 )); then\n"
            + "             time=0\n"
            + "         fi\n"
            + "		while IFS='' read -r line || [[ -n \"$line\" ]]; do\n"
            + "                 if [[ \"$line\" =~ ^[0-9]+,[0-9]{0,9}:[0-9]+[\\.]{0,1}[0-9]{0,3}:[0-9]+ ]]; then\n"
            + "  			IFS=':' read -ra ADDR <<< \"$line\"\n"
            + "				cont=0\n"
            + "				Ntem=1\n"
            + "				for i in \"${ADDR[@]}\"; do\n"
            + "					((cont++))\n"
            + "					if [[ $cont == 1 ]] ; then\n"
            + "            				pid=${i%,*}\n"
            + "            				pidR=${i#*,}\n"
            + "						grupo=\"" + prefixoDoGrupo + "$pid\"\n"
            + "						for item in ${pids[*]}; do\n"
            + "							if [[ $item == $pid ]] ; then\n"
            + "								Ntem=0\n"
            + "								if (( $time > 4 )); then\n"
            + "									if [ -d \"/proc/$pid/task/\" ]; then\n"
            + "										for f in /proc/$pid/task/* ; do\n"
            + "											f=${f##*/}\n"
            + "											ntem=1\n"
            + "											for item2 in ${pids[*]}; do\n"
            + "												if [[ $item2 == $f ]] ; then\n"
            + "													ntem=0\n"
            + "												fi\n"
            + "											done\n"
            + "                                                                                 if [[ $ntem == 1 ]]; then\n"
            + "                                                                                         echo \"adicionando tarefa $f ao grupo $grupo\"\n"
            + "												sudo echo \"$f\" >> $cgPath$cpuPath$grupo/tasks\n"
            + "												sudo echo \"$f\" >> $cgPath$memPath$grupo/tasks\n"
            + "												pids[${#pids[*]}]=$f\n"
            + "											fi\n"
            + "										done\n"
            + "									fi\n"
            + "                                                                 if [ \"$pid\" != \"$pidR\" ]; then\n"
            + "                                                                     if [ -d \"/proc/$pidR/task/\" ]; then\n"
            + "										for f in /proc/$pidR/task/* ; do\n"
            + "											f=${f##*/}\n"
            + "											ntem=1\n"
            + "											for item2 in ${pids[*]}; do\n"
            + "												if [[ $item2 == $f ]] ; then\n"
            + "													ntem=0\n"
            + "												fi\n"
            + "											done\n"
            + "											if [[ $ntem == 1 ]]; then\n"
            + "                                                                                         echo \"adicionando tarefa $f ao grupo $grupo\"\n"
            + "												sudo echo \"$f\" >> $cgPath$cpuPath$grupo/tasks\n"
            + "												sudo echo \"$f\" >> $cgPath$memPath$grupo/tasks\n"
            + "												pids[${#pids[*]}]=$f\n"
            + "											fi\n"
            + "										done\n"
            + "                                                                     fi\n"
            + "									fi\n"
            + "								fi\n"
            + "							fi\n"
            + "						done\n"
            + "						if [[ $Ntem == 1 ]]; then\n"
            + "                                         echo \"Linha: $i pid: $pid pidR: $pidR grupo: $grupo\"\n"
            + "                                            if [ -d \"/proc/$pid/\" ]; then\n"
            + "							echo \"------------------------------------------\"\n"
            + "							echo \"a tratar: PID $pid GRUPO: $grupo\"\n"
            + "							sudo cgcreate -g cpu,memory:/$grupo\n"
            + "							#o comando acima é equivalente a:\n"
            + "							#sudo mkdir $cgPath$cpuPath$grupo\n"
            + "							#sudo mkdir $cgPath$memPath$grupo\n"
            + "                                             else\n"
            + "                                                  echo \"!! processo PID $pid não existe!\"\n"
            + "                                                  pids[${#pids[*]}]=$pid\n"
            + "                                                  Ntem=0\n"
            + "                                            fi\n"
            + "						fi\n"
            + "					fi\n"
            + "					################################################################ CONFIGURAR CPU\n"
            + "					if [[ $Ntem == 1 && $cont == 2 ]] ; then\n"
            + "						quota=$(bc <<<\"1000 * $i\")\n"
            + "						quota=${quota%\\.*}\n"
            + "						period=100000\n"
            + "						dec=0\n"
            + "						if (( quota < 1000 )) ; then\n"
            + "							tmp=${i#*\\.}\n"
            + "							dec=${#tmp}\n"
            + "							period=$(bc <<<\"$period / $i\")\n"
            + "							quota=1000\n"
            + "							pre=\"0\"\n"
            + "						else\n"
            + "							pre=\"\"\n"
            + "						fi\n"
            + "						sudo cgset -r cpu.cfs_quota_us=$quota $grupo\n"
            + "						sudo cgset -r cpu.cfs_period_us=$period $grupo\n"
            + "						#o comando acima é equivalente a:\n"
            + "						#echo $cgPath$cpuPath$grupo/$quota > cpu.cfs_quota_us\n"
            + "						#echo $cgPath$cpuPath$grupo/$period > cpu.cfs_period_us\n"
            + "						qt=$( cat $cgPath$cpuPath$grupo/cpu.cfs_quota_us )\n"
            + "						pd=$( cat $cgPath$cpuPath$grupo/cpu.cfs_period_us )\n"
            + "						percent=$(bc <<<\"scale=$dec;$qt * 100 / $pd\")		\n"
            + "                                         echo 1 > $cgPath$cpuPath$grupo/cgroup.clone_children\n"
            + "						echo \"CPU limit: de $i% atribuido $pre$percent%\"\n"
            + "					fi\n"
            + "					################################################################ CONFIGURAR MEMORIA\n"
            + "					if [[ $Ntem == 1 && $cont == 3 ]] ; then\n"
            + "						mem=$i\n"
            + "						sudo cgset -r memory.limit_in_bytes=$mem$memSize $grupo\n"
            + "						sudo cgset -r memory.memsw.limit_in_bytes=$mem$memSize $grupo\n"
            + "						#os comandos acima são equivalentes a:\n"
            + "						#echo $mem > $cgPath$memPath$grupo/memory.limit_in_bytes\n"
            + "						#echo $mem > $cgPath$memPath$grupo/memory.memsw.limit_in_bytes\n"
            + "						nmem=$( cat $cgPath$memPath$grupo/memory.memsw.limit_in_bytes )\n"
            + "                                         echo 1 > $cgPath$memPath$grupo/cgroup.clone_children\n"
            + "						echo \"MEMORY limit: de $i MB atribuido $(( $nmem / 1048576 )) MB\"\n"
            + "						################################################################ VERIFICAR E ATRIBUIR\n"
            + "						sudo cgclassify -g cpu,memory:$grupo --sticky $pid $pidR\n"
            + "						#o comando acima é equivalente a:\n"
            + "						#echo $pid > $cgPath$grupo$memPath/tasks ; echo $pid > $cgPath$grupo$cpuPath/tasks\n"
            + "						echo \"CLASSIFICADO PID $pid $grupo\"\n"
            + "						cc=$( cat /proc/$pid/cgroup | grep cpu.*$grupo -o -m 1 )\n"
            + "						cc2=${#cc} \n"
            + "						mm=$( cat /proc/$pid/cgroup | grep mem.*$grupo -o -m 1 )\n"
            + "						mm2=${#mm}\n"
            + "						gg=${#grupo}\n"
            + "						if (( mm2 > gg && cc2 > gg )) ; then\n"
            + "							pids[${#pids[*]}]=$pid\n"
            + "							touch " + MainSingleton.DIRETORIO + pathTemp + "$grupo\n"
            + "							echo \"o grupo $grupo será excluído na reinicialização.\"\n"
            + "						else\n"
            + "							echo \"ERRO AO TENTAR CONFIGURAR PID $pid KILLED ?\"\n"
            + "						fi\n"
            + "						##################################################################\n"
            + "					fi\n"
            + "				done\n"
            + "			fi\n"
            + "                 if [[ \"$line\" =~ 'kill'[0-9]+ ]]; then\n"
            + "				pid=${line#kill}\n"
            + "				if [ -d \"/proc/$pid/task/\" ]; then\n"
            + "					for f in /proc/$pid/task/* ; do\n"
            + "						f=${f##*/}\n"
            + "						echo \"terminando processo $f\"\n"
            + "						kill -9 $f\n"
            + "					done\n"
            + "					kill -9 $pid\n"
            + "				fi\n"
            + "			fi\n"
            + "                 if [[ \"$line\" =~ [a-z]+:'" + prefixoDoGrupo + "'[0-9]+:[0-9]+[\\.]{0,1}[0-9]{0,3}:[0-9]+:* ]]; then\n"
            + "				IFS=':' read -ra ADDR <<< \"$line\"\n"
            + "				cont=0\n"
            + "				Ntem=2\n"
            + "				for i in \"${ADDR[@]}\"; do\n"
            + "					((cont++))\n"
            + "					if [[ $cont == 1 ]] ; then\n"
            + "                                         atualizacao=$i\n"
            + "						for item in ${grupos[*]}; do\n"
            + "							if [[ $item == $atualizacao ]] ; then\n"
            + "								Ntem=0\n"
            + "                                                 fi\n"
            + "                                          done\n"
            + "                                          if [[ $Ntem == 2 ]] ; then\n"
            + "                                             grupos[${#grupos[*]}]=$atualizacao\n"
            + "                                          fi\n"
            + "					fi\n"
            + "					if [[  $Ntem == 2 && $cont == 2 ]] ; then\n"
            + "                                         grupo=$i\n"
            + "                                          if [ ! -d \"$cgPath$memPath$grupo\" ]; then\n"
            + "                                                 echo \"grupo $grupo não existe!\"\n"
            + "                                                 Ntem=1\n"
            + "                                          fi\n"
            + "					fi\n"
            + "					if [[ $Ntem == 2 && $cont == 3 ]] ; then\n"
            + "						quota=$(bc <<<\"1000 * $i\")\n"
            + "						quota=${quota%\\.*}\n"
            + "						period=100000\n"
            + "						dec=0\n"
            + "						if (( quota < 1000 )) ; then\n"
            + "							tmp=${i#*\\.}\n"
            + "							dec=${#tmp}\n"
            + "							period=$(bc <<<\"$period / $i\")\n"
            + "							quota=1000\n"
            + "							pre=\"0\"\n"
            + "						else\n"
            + "							pre=\"\"\n"
            + "						fi\n"
            + "						sudo cgset -r cpu.cfs_quota_us=$quota $grupo\n"
            + "						sudo cgset -r cpu.cfs_period_us=$period $grupo\n"
            + "						qt=$( cat $cgPath$cpuPath$grupo/cpu.cfs_quota_us )\n"
            + "						pd=$( cat $cgPath$cpuPath$grupo/cpu.cfs_period_us )\n"
            + "						percent=$(bc <<<\"scale=$dec;$qt * 100 / $pd\")\n"
            + "                                         echo 1 > $cgPath$cpuPath$grupo/cgroup.clone_children\n"
            + "						echo \"atualizacao: $atualizacao de $grupo CPU limit atualizado: de $i% atribuido $pre$percent%\"\n"
            + "					fi\n"
            + "					if [[ $Ntem == 2 && $cont == 4 ]] ; then\n"
            + "						mem=$i\n"
            + "						sudo cgset -r memory.limit_in_bytes=$mem$memSize $grupo\n"
            + "						sudo cgset -r memory.memsw.limit_in_bytes=$mem$memSize $grupo\n"
            + "						nmem=$( cat $cgPath$memPath$grupo/memory.memsw.limit_in_bytes )\n"
            + "                                         echo 1 > $cgPath$memPath$grupo/cgroup.clone_children\n"
            + "						echo \"atualizacao: $atualizacao de $grupo MEMORY limit atualizado: de $i MB atribuido $(( $nmem / 1048576 )) MB\"\n"
            + "					fi\n"
            + "					if [[ $Ntem > 0 && $cont == 5 ]] ; then\n"
            + "						eval $i\n"
            + "						echo \"atualizacao: $atualizacao de $grupo comando executado: $i\"\n"
            + "                                         touch " + MainSingleton.DIRETORIO + pathTemp + "$atualizacao\n"
            + "					fi\n"
            + "				done\n"
            + "			fi\n"
            + "		done < \"$pidArquivo\"\n"
            + "		sleep " + timeToReloadFile + ";\n"
            + "	done\n"
            + "else\n"
            + "	echo \"Tentando instalar o programa cpulimit E libcgroup-tools em FEDORA!, após terminar tente novamente.\"\n"
            + "	read -t 5\n"
            + "	sudo dnf install cpulimit"
            + "	sudo dnf install libcgroup-tools\n"
            + "fi\n"
            + "exit";

}
