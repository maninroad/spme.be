package com.spme.controller;

import com.spme.domain.JobOutputListItem;
import com.spme.service.JclService;
import com.spme.utils.AuthUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

/**
 * @author Qingguo Li
 */
@Controller
public class JclController {

    @Resource
    private JclService js;

    /**
     * submit a JCL job and get response
     */
    @CrossOrigin(origins = "*", allowCredentials = "true")
    @RequestMapping(value = "/jcl", method = RequestMethod.POST)
    public ResponseEntity<List<JobOutputListItem>> submitJCL(@RequestBody Map<String, String> body, HttpSession session) {
        if (AuthUtil.notLogin(session)) {
            return ResponseEntity.status(401).body(null);
        }
        List<JobOutputListItem> res;
        if ((res = js.submitJCL(session, body.get("jcl"))) != null) {
            return ResponseEntity.ok(res);
        } else {
            // time out
            return ResponseEntity.status(202).body(null);
        }
    }
    @CrossOrigin(origins = "*", allowCredentials = "true")
    @RequestMapping(value = "/db2jcl", method = RequestMethod.POST)
    public ResponseEntity<List<JobOutputListItem>> submitDB2JCL(@RequestBody Map<String, String> body, HttpSession session) {
        if (AuthUtil.notLogin(session)) {
            return ResponseEntity.status(401).body(null);
        }
        System.out.println(body.get("jcl"));
        String head="//EXECSQL JOB NOTIFY=&SYSUID \n"
                +"// SET DB2LOAD=DSNA10.SDSNLOAD  \n"
                +"//JOBLIB  DD  DSN=&DB2LOAD,DISP=SHR  \n"
                +"//BIND  EXEC PGM=IKJEFT01,DYNAMNBR=20 \n"
                +"//SYSTSPRT DD SYSOUT=* \n"
                +"//SYSPRINT DD SYSOUT=* \n"
                +"//SYSUDUMP DD SYSOUT=* \n"
                +"//SYSOUT   DD SYSOUT=* \n"
                +"//SYSTSIN  DD *  \n"
                +"   DSN SYSTEM(DP10)    \n"
                +"   RUN  PROGRAM(DSNTIAD) PLAN(DSNTIAA1) -  \n"
                +"    LIB('DSNA10.DP10.RUNLIB.LOAD')    \n"
                +"//SYSIN    DD * \n"+"  ";
        List<JobOutputListItem> res;
        if ((res = js.submitJCL(session, head+body.get("jcl"))) != null) {
            return ResponseEntity.ok(res);
        } else {
            // time out
            return ResponseEntity.status(202).body(null);
        }
    }
}
