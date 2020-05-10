package com.spme.controller;


import com.spme.domain.*;
import com.spme.service.SmsService;
import com.spme.utils.AuthUtil;
import com.spme.utils.SslUtil;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * @author Qingguo Li
 */
@Controller
public class SmsController {

    @Resource
    private SmsService ss;

    //ISMF 11.1
    @Deprecated
    @CrossOrigin(origins = "*", allowCredentials = "true")
    @RequestMapping(value = "/sms/ismf/11/1", method = RequestMethod.POST)
    public ResponseEntity<JCLInfo> subJob111(@RequestBody Map<String, String> map, HttpSession session) {
        Object ZOSMF_JSESSIONID = session.getAttribute("ZOSMF_JSESSIONID");
        Object ZOSMF_LtpaToken2 = session.getAttribute("ZOSMF_LtpaToken2");
        Object ZOSMF_Address = session.getAttribute("ZOSMF_Address");

        if (ZOSMF_JSESSIONID == null || ZOSMF_LtpaToken2 == null || ZOSMF_Address == null) {
            return new ResponseEntity("unauthorized", HttpStatus.valueOf(401));
        } else {
            CloseableHttpClient httpClient = SslUtil.SslHttpClientBuild();
            HttpComponentsClientHttpRequestFactory requestFactory
                    = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setHttpClient(httpClient);
            //jcljob提交地址
            String jclAddress = ZOSMF_Address.toString();
            String urlOverHttps = "https://" + jclAddress + "/zosmf/restjobs/jobs";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.add("Cookie", ZOSMF_JSESSIONID.toString() + ";" + ZOSMF_LtpaToken2);
            //body
            //获取参数
            String id = map.get("id");
            String Scds = map.get("scds");
            String srcName = map.get("srcName");
            String memName = map.get("memName");
            String listName = map.get("listName");

            StringBuffer sb = new StringBuffer();
            sb.append("//ST010111 JOB (ACCT),'ST010',MSGCLASS=H,\n");
            sb.append("//      NOTIFY=ST010,CLASS=A,MSGLEVEL=(1,1),TIME=(0,10)\n");
            sb.append("//MYLIB JCLLIB ORDER=SYS1.SACBCNTL\n");
            sb.append("//STEP1   EXEC ACBJBAOB,\n");
            sb.append("//         PLIB1='SYS1.DGTPLIB',\n");
            sb.append("//         TABL2=ST010.TEST.ISPTABL\n");
            sb.append("//SYSTSIN  DD *\n");
            sb.append("PROFILE PREFIX(ST010)\n");
            sb.append("ISPSTART CMD(ACBQBAO1 +\n");
            sb.append("ACSSRC('ST010.SMS.ACS') MEMBER(SC) +\n");
            sb.append("SCDS('ST010.SMS.SCSDS') LISTNAME(ST010.LIST) +\n");
            sb.append("UPDHLVLSCDS()) +\n");
            sb.append("NEWAPPL(DGT) BATSCRW(132) BATSCRD(27) BREDIMAX(3) BDISPMAX(99999999)\n");
            sb.append("/*");


            //提交jcl的request
            HttpEntity<String> requestSub = new HttpEntity<>(sb.toString(), headers);
            //响应内容
            ResponseEntity<JobInfo> responseSub = new RestTemplate(requestFactory).exchange(urlOverHttps, HttpMethod.PUT, requestSub, JobInfo.class);

            //query job's status
            for (int i = 0; i < 10; i++) {
                try {
                    Thread.currentThread().sleep(1000);//
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                //查询执行状态的地址
                urlOverHttps = "https://" + jclAddress + "/zosmf/restjobs/jobs/" + responseSub.getBody().getJobName() + "/" + responseSub.getBody().getJobId();
                //查询结果
                HttpEntity<String> requestQur = new HttpEntity<>(headers);
                ResponseEntity<JobInfo> responseQur = new RestTemplate(requestFactory).exchange(urlOverHttps, HttpMethod.GET, requestQur, JobInfo.class);
                //判断作业状态
                if (responseQur.getBody().getStatus().equals("OUTPUT")) {
                    //查询执行结果的地址
                    JCLInfo res_jclinfo = new JCLInfo();
                    String JESMSGLG_url = urlOverHttps + "/files/2/records";
                    String JESJCL_url = urlOverHttps + "/files/3/records";
                    String JESYSMSG_url = urlOverHttps + "/files/4/records";
                    String SYSPRINT_url = urlOverHttps + "/files/102/records";
                    ResponseEntity<String> res_JESMSGLG = new RestTemplate(requestFactory).exchange(JESMSGLG_url, HttpMethod.GET, requestQur, String.class);
                    res_jclinfo.setJESMSGLG(res_JESMSGLG.getBody());
                    ResponseEntity<String> res_JESJCL = new RestTemplate(requestFactory).exchange(JESJCL_url, HttpMethod.GET, requestQur, String.class);
                    res_jclinfo.setJESJCL(res_JESJCL.getBody());
                    ResponseEntity<String> res_JESYSMSG = new RestTemplate(requestFactory).exchange(JESYSMSG_url, HttpMethod.GET, requestQur, String.class);
                    res_jclinfo.setJESYSMSG(res_JESYSMSG.getBody());
//                    ResponseEntity<String> res_SYSPRINT = new RestTemplate(requestFactory).exchange(SYSPRINT_url, HttpMethod.GET, requestQur, String.class);
//                    res_jclinfo.setSYSPRINT(res_SYSPRINT.getBody());
                    return new ResponseEntity<JCLInfo>(res_jclinfo, HttpStatus.OK);
                }
            }
            //超时
            return new ResponseEntity("time out", HttpStatus.valueOf(202));
        }
    }

    //ISMF 11.2
    @Deprecated
    @CrossOrigin(origins = "*", allowCredentials = "true")
    @RequestMapping(value = "/sms/ismf/11/2", method = RequestMethod.POST)
    public ResponseEntity<JCLInfo> subJob112(@RequestBody Map<String, String> map, HttpSession session) {
        Object ZOSMF_JSESSIONID = session.getAttribute("ZOSMF_JSESSIONID");
        Object ZOSMF_LtpaToken2 = session.getAttribute("ZOSMF_LtpaToken2");
        Object ZOSMF_Address = session.getAttribute("ZOSMF_Address");

        if (ZOSMF_JSESSIONID == null || ZOSMF_LtpaToken2 == null || ZOSMF_Address == null) {
            return new ResponseEntity("unauthorized", HttpStatus.valueOf(401));
        } else {
            CloseableHttpClient httpClient = SslUtil.SslHttpClientBuild();
            HttpComponentsClientHttpRequestFactory requestFactory
                    = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setHttpClient(httpClient);
            //jcljob提交地址
            String jclAddress = ZOSMF_Address.toString();
            String urlOverHttps = "https://" + jclAddress + "/zosmf/restjobs/jobs";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.add("Cookie", ZOSMF_JSESSIONID.toString() + ";" + ZOSMF_LtpaToken2);
            //body
            //get paragram
            String id = map.get("id");
            String Scds = map.get("scds");
            String srcName = map.get("srcName");
            String memName = map.get("memName");
            String listName = map.get("listName");

            StringBuffer sb = new StringBuffer();
            sb.append("//ST010111 JOB (ACCT),'ST010',MSGCLASS=H,\n");
            sb.append("//      NOTIFY=ST010,CLASS=A,MSGLEVEL=(1,1),TIME=(0,10)\n");
            sb.append("//MYLIB JCLLIB ORDER=SYS1.SACBCNTL\n");
            sb.append("//STEP1   EXEC ACBJBAOB,\n");
            sb.append("//         PLIB1='SYS1.DGTPLIB',\n");
            sb.append("//         TABL2=ST010.TEST.ISPTABL\n");
            sb.append("//SYSTSIN  DD *\n");
            sb.append("PROFILE PREFIX(ST010)\n");
            sb.append("ISPSTART CMD(ACBQBAO1 +\n");
            sb.append("ACSSRC('ST010.SMS.ACS') MEMBER(DC) +\n");
            sb.append("SCDS('ST010.SMS.SCSDS') LISTNAME(ST010.LIST) +\n");
            sb.append("UPDHLVLSCDS()) +\n");
            sb.append("NEWAPPL(DGT) BATSCRW(132) BATSCRD(27) BREDIMAX(3) BDISPMAX(99999999)\n");
            sb.append("/*");


            //提交jcl的request
            HttpEntity<String> requestSub = new HttpEntity<>(sb.toString(), headers);
            //响应内容
            ResponseEntity<JobInfo> responseSub = new RestTemplate(requestFactory).exchange(urlOverHttps, HttpMethod.PUT, requestSub, JobInfo.class);

            //query job's status
            for (int i = 0; i < 10; i++) {
                try {
                    Thread.currentThread().sleep(1000);//
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                //查询执行状态的地址
                urlOverHttps = "https://" + jclAddress + "/zosmf/restjobs/jobs/" + responseSub.getBody().getJobName() + "/" + responseSub.getBody().getJobId();
                //查询结果
                HttpEntity<String> requestQur = new HttpEntity<>(headers);
                ResponseEntity<JobInfo> responseQur = new RestTemplate(requestFactory).exchange(urlOverHttps, HttpMethod.GET, requestQur, JobInfo.class);
                //判断作业状态
                if (responseQur.getBody().getStatus().equals("OUTPUT")) {
                    //查询执行结果的地址
                    JCLInfo res_jclinfo = new JCLInfo();
                    String JESMSGLG_url = urlOverHttps + "/files/2/records";
                    String JESJCL_url = urlOverHttps + "/files/3/records";
                    String JESYSMSG_url = urlOverHttps + "/files/4/records";
                    String SYSPRINT_url = urlOverHttps + "/files/102/records";
                    ResponseEntity<String> res_JESMSGLG = new RestTemplate(requestFactory).exchange(JESMSGLG_url, HttpMethod.GET, requestQur, String.class);
                    res_jclinfo.setJESMSGLG(res_JESMSGLG.getBody());
                    ResponseEntity<String> res_JESJCL = new RestTemplate(requestFactory).exchange(JESJCL_url, HttpMethod.GET, requestQur, String.class);
                    res_jclinfo.setJESJCL(res_JESJCL.getBody());
                    ResponseEntity<String> res_JESYSMSG = new RestTemplate(requestFactory).exchange(JESYSMSG_url, HttpMethod.GET, requestQur, String.class);
                    res_jclinfo.setJESYSMSG(res_JESYSMSG.getBody());
//                    ResponseEntity<String> res_SYSPRINT = new RestTemplate(requestFactory).exchange(SYSPRINT_url, HttpMethod.GET, requestQur, String.class);
//                    res_jclinfo.setSYSPRINT(res_SYSPRINT.getBody());
                    return new ResponseEntity<JCLInfo>(res_jclinfo, HttpStatus.OK);
                }
            }
            //超时
            return new ResponseEntity("time out", HttpStatus.valueOf(202));
        }
    }

    //ISMF 11.3
    @Deprecated
    @CrossOrigin(origins = "*", allowCredentials = "true")
    @RequestMapping(value = "/sms/ismf/11/3", method = RequestMethod.POST)
    public ResponseEntity<JCLInfo> subJob113(@RequestBody Map<String, String> map, HttpSession session) {
        Object ZOSMF_JSESSIONID = session.getAttribute("ZOSMF_JSESSIONID");
        Object ZOSMF_LtpaToken2 = session.getAttribute("ZOSMF_LtpaToken2");
        Object ZOSMF_Address = session.getAttribute("ZOSMF_Address");

        if (ZOSMF_JSESSIONID == null || ZOSMF_LtpaToken2 == null || ZOSMF_Address == null) {
            return new ResponseEntity("unauthorized", HttpStatus.valueOf(401));
        } else {
            CloseableHttpClient httpClient = SslUtil.SslHttpClientBuild();
            HttpComponentsClientHttpRequestFactory requestFactory
                    = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setHttpClient(httpClient);
            //jcljob提交地址
            String jclAddress = ZOSMF_Address.toString();
            String urlOverHttps = "https://" + jclAddress + "/zosmf/restjobs/jobs";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.add("Cookie", ZOSMF_JSESSIONID.toString() + ";" + ZOSMF_LtpaToken2);
            //body
            //get para
            String id = map.get("id");
            String Scds = map.get("scds");
            String listName = map.get("listName");
            String memName = map.get("memName");
            String srcName = map.get("srcName");
            StringBuffer sb = new StringBuffer();
            sb.append("//ST010111 JOB (ACCT),'ST010',MSGCLASS=H,\n");
            sb.append("//      NOTIFY=ST010,CLASS=A,MSGLEVEL=(1,1),TIME=(0,10)\n");
            sb.append("//MYLIB JCLLIB ORDER=SYS1.SACBCNTL\n");
            sb.append("//STEP1   EXEC ACBJBAOB,\n");
            sb.append("//         PLIB1='SYS1.DGTPLIB',\n");
            sb.append("//         TABL2=ST010.TEST.ISPTABL\n");
            sb.append("//SYSTSIN  DD *\n");
            sb.append("PROFILE PREFIX(ST010)\n");
            sb.append("ISPSTART CMD(ACBQBAO1 +\n");
            sb.append("ACSSRC('ST010.SMS.ACS') MEMBER(MC) +\n");
            sb.append("SCDS('ST010.SMS.SCSDS') LISTNAME(ST010.LIST) +\n");
            sb.append("UPDHLVLSCDS()) +\n");
            sb.append("NEWAPPL(DGT) BATSCRW(132) BATSCRD(27) BREDIMAX(3) BDISPMAX(99999999)\n");
            sb.append("/*");


            //提交jcl的request
            HttpEntity<String> requestSub = new HttpEntity<>(sb.toString(), headers);
            //响应内容
            ResponseEntity<JobInfo> responseSub = new RestTemplate(requestFactory).exchange(urlOverHttps, HttpMethod.PUT, requestSub, JobInfo.class);

            //query job's status
            for (int i = 0; i < 10; i++) {
                try {
                    Thread.currentThread().sleep(1000);//
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                //查询执行状态的地址
                urlOverHttps = "https://" + jclAddress + "/zosmf/restjobs/jobs/" + responseSub.getBody().getJobName() + "/" + responseSub.getBody().getJobId();
                //查询结果
                HttpEntity<String> requestQur = new HttpEntity<>(headers);
                ResponseEntity<JobInfo> responseQur = new RestTemplate(requestFactory).exchange(urlOverHttps, HttpMethod.GET, requestQur, JobInfo.class);
                //判断作业状态
                if (responseQur.getBody().getStatus().equals("OUTPUT")) {
                    //查询执行结果的地址
                    JCLInfo res_jclinfo = new JCLInfo();
                    String JESMSGLG_url = urlOverHttps + "/files/2/records";
                    String JESJCL_url = urlOverHttps + "/files/3/records";
                    String JESYSMSG_url = urlOverHttps + "/files/4/records";
                    String SYSPRINT_url = urlOverHttps + "/files/102/records";
                    ResponseEntity<String> res_JESMSGLG = new RestTemplate(requestFactory).exchange(JESMSGLG_url, HttpMethod.GET, requestQur, String.class);
                    res_jclinfo.setJESMSGLG(res_JESMSGLG.getBody());
                    ResponseEntity<String> res_JESJCL = new RestTemplate(requestFactory).exchange(JESJCL_url, HttpMethod.GET, requestQur, String.class);
                    res_jclinfo.setJESJCL(res_JESJCL.getBody());
                    ResponseEntity<String> res_JESYSMSG = new RestTemplate(requestFactory).exchange(JESYSMSG_url, HttpMethod.GET, requestQur, String.class);
                    res_jclinfo.setJESYSMSG(res_JESYSMSG.getBody());
//                    ResponseEntity<String> res_SYSPRINT = new RestTemplate(requestFactory).exchange(SYSPRINT_url, HttpMethod.GET, requestQur, String.class);
//                    res_jclinfo.setSYSPRINT(res_SYSPRINT.getBody());
                    return new ResponseEntity<JCLInfo>(res_jclinfo, HttpStatus.OK);
                }
            }
            //超时
            return new ResponseEntity("time out", HttpStatus.valueOf(202));
        }
    }


    //ISMF 11.4
    @Deprecated
    @CrossOrigin(origins = "*", allowCredentials = "true")
    @RequestMapping(value = "/sms/ismf/11/4", method = RequestMethod.POST)
    public ResponseEntity<JCLInfo> subJob114(@RequestBody Map<String, String> map, HttpSession session) {
        Object ZOSMF_JSESSIONID = session.getAttribute("ZOSMF_JSESSIONID");
        Object ZOSMF_LtpaToken2 = session.getAttribute("ZOSMF_LtpaToken2");
        Object ZOSMF_Address = session.getAttribute("ZOSMF_Address");

        if (ZOSMF_JSESSIONID == null || ZOSMF_LtpaToken2 == null || ZOSMF_Address == null) {
            return new ResponseEntity("unauthorized", HttpStatus.valueOf(401));
        } else {
            CloseableHttpClient httpClient = SslUtil.SslHttpClientBuild();
            HttpComponentsClientHttpRequestFactory requestFactory
                    = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setHttpClient(httpClient);
            //jcljob提交地址
            String jclAddress = ZOSMF_Address.toString();
            String urlOverHttps = "https://" + jclAddress + "/zosmf/restjobs/jobs";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.add("Cookie", ZOSMF_JSESSIONID.toString() + ";" + ZOSMF_LtpaToken2);
            //body
            String id = map.get("id");
            String Scds = map.get("scds");
            String srcName = map.get("srcName");
            String memName = map.get("memName");
            String listName = map.get("listName");

            StringBuffer sb = new StringBuffer();
            sb.append("//ST010111 JOB (ACCT),'ST010',MSGCLASS=H,\n");
            sb.append("//      NOTIFY=ST010,CLASS=A,MSGLEVEL=(1,1),TIME=(0,10)\n");
            sb.append("//MYLIB JCLLIB ORDER=SYS1.SACBCNTL\n");
            sb.append("//STEP1   EXEC ACBJBAOB,\n");
            sb.append("//         PLIB1='SYS1.DGTPLIB',\n");
            sb.append("//         TABL2=ST010.TEST.ISPTABL\n");
            sb.append("//SYSTSIN  DD *\n");
            sb.append("PROFILE PREFIX(ST010)\n");
            sb.append("ISPSTART CMD(ACBQBAO1 +\n");
            sb.append("ACSSRC('ST010.SMS.ACS') MEMBER(SG) +\n");
            sb.append("SCDS('ST010.SMS.SCSDS') LISTNAME(ST010.LIST) +\n");
            sb.append("UPDHLVLSCDS()) +\n");
            sb.append("NEWAPPL(DGT) BATSCRW(132) BATSCRD(27) BREDIMAX(3) BDISPMAX(99999999)\n");
            sb.append("/*");

            //提交jcl的request
            HttpEntity<String> requestSub = new HttpEntity<>(sb.toString(), headers);
            //响应内容
            ResponseEntity<JobInfo> responseSub = new RestTemplate(requestFactory).exchange(urlOverHttps, HttpMethod.PUT, requestSub, JobInfo.class);

            //query job's status
            for (int i = 0; i < 10; i++) {
                try {
                    Thread.currentThread().sleep(1000);//
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                //查询执行状态的地址
                urlOverHttps = "https://" + jclAddress + "/zosmf/restjobs/jobs/" + responseSub.getBody().getJobName() + "/" + responseSub.getBody().getJobId();
                //查询结果
                HttpEntity<String> requestQur = new HttpEntity<>(headers);
                ResponseEntity<JobInfo> responseQur = new RestTemplate(requestFactory).exchange(urlOverHttps, HttpMethod.GET, requestQur, JobInfo.class);
                //判断作业状态
                if (responseQur.getBody().getStatus().equals("OUTPUT")) {
                    //查询执行结果的地址
                    JCLInfo res_jclinfo = new JCLInfo();
                    String JESMSGLG_url = urlOverHttps + "/files/2/records";
                    String JESJCL_url = urlOverHttps + "/files/3/records";
                    String JESYSMSG_url = urlOverHttps + "/files/4/records";
                    String SYSPRINT_url = urlOverHttps + "/files/102/records";
                    ResponseEntity<String> res_JESMSGLG = new RestTemplate(requestFactory).exchange(JESMSGLG_url, HttpMethod.GET, requestQur, String.class);
                    res_jclinfo.setJESMSGLG(res_JESMSGLG.getBody());
                    ResponseEntity<String> res_JESJCL = new RestTemplate(requestFactory).exchange(JESJCL_url, HttpMethod.GET, requestQur, String.class);
                    res_jclinfo.setJESJCL(res_JESJCL.getBody());
                    ResponseEntity<String> res_JESYSMSG = new RestTemplate(requestFactory).exchange(JESYSMSG_url, HttpMethod.GET, requestQur, String.class);
                    res_jclinfo.setJESYSMSG(res_JESYSMSG.getBody());
//                    ResponseEntity<String> res_SYSPRINT = new RestTemplate(requestFactory).exchange(SYSPRINT_url, HttpMethod.GET, requestQur, String.class);
//                    res_jclinfo.setSYSPRINT(res_SYSPRINT.getBody());
                    return new ResponseEntity<JCLInfo>(res_jclinfo, HttpStatus.OK);
                }
            }
            //超时
            return new ResponseEntity("time out", HttpStatus.valueOf(202));
        }
    }

    //ISMF 12.1
    @Deprecated
    @CrossOrigin(origins = "*", allowCredentials = "true")
    @RequestMapping(value = "/sms/ismf/12/1", method = RequestMethod.POST)
    public ResponseEntity<JCLInfo> subJob121(@RequestBody Map<String, String> map, HttpSession session) {
        Object ZOSMF_JSESSIONID = session.getAttribute("ZOSMF_JSESSIONID");
        Object ZOSMF_LtpaToken2 = session.getAttribute("ZOSMF_LtpaToken2");
        Object ZOSMF_Address = session.getAttribute("ZOSMF_Address");

        if (ZOSMF_JSESSIONID == null || ZOSMF_LtpaToken2 == null || ZOSMF_Address == null) {
            return new ResponseEntity("unauthorized", HttpStatus.valueOf(401));
        } else {
            CloseableHttpClient httpClient = SslUtil.SslHttpClientBuild();
            HttpComponentsClientHttpRequestFactory requestFactory
                    = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setHttpClient(httpClient);
            //jcljob提交地址
            String jclAddress = ZOSMF_Address.toString();
            String urlOverHttps = "https://" + jclAddress + "/zosmf/restjobs/jobs";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.add("Cookie", ZOSMF_JSESSIONID.toString() + ";" + ZOSMF_LtpaToken2);
            //body
            String id = map.get("id");
            String Scds = map.get("scds");
            StringBuffer sb = new StringBuffer();
            sb.append("//ST010112 JOB (ACCT),'ST010',MSGCLASS=H,\n");
            sb.append("//      NOTIFY=ST010,CLASS=A,MSGLEVEL=(1,1),TIME=(0,10)\n");
            sb.append("//MYLIB JCLLIB ORDER=SYS1.SACBCNTL\n");
            sb.append("//TRANSLAT EXEC ACBJBAOB,\n");
            sb.append("//         PLIB1='SYS1.DGTPLIB',\n");
            sb.append("//         TABL2=ST010.TEST.ISPTABL\n");
            sb.append("//SYSTSIN  DD *\n");
            sb.append("PROFILE PREFIX(ST010)\n");
            sb.append("ISPSTART CMD(ACBQBAO2 SCDS('ST010.SMS.SCDS') TYPE(*) +\n");
            sb.append("LISTNAME('ST010.LIST1') +\n");
            sb.append("UPDHLVLSCDS()) +\n");
            sb.append("NEWAPPL(DGT) BATSCRW(132) BATSCRD(27) BREDIMAX(3) BDISPMAX(99999999)\n");
            sb.append("/*");

            //提交jcl的request
            HttpEntity<String> requestSub = new HttpEntity<>(sb.toString(), headers);
            //响应内容
            ResponseEntity<JobInfo> responseSub = new RestTemplate(requestFactory).exchange(urlOverHttps, HttpMethod.PUT, requestSub, JobInfo.class);

            //query job's status
            for (int i = 0; i < 10; i++) {
                try {
                    Thread.currentThread().sleep(1000);//
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                //查询执行状态的地址
                urlOverHttps = "https://" + jclAddress + "/zosmf/restjobs/jobs/" + responseSub.getBody().getJobName() + "/" + responseSub.getBody().getJobId();
                //查询结果
                HttpEntity<String> requestQur = new HttpEntity<>(headers);
                ResponseEntity<JobInfo> responseQur = new RestTemplate(requestFactory).exchange(urlOverHttps, HttpMethod.GET, requestQur, JobInfo.class);
                //判断作业状态
                if (responseQur.getBody().getStatus().equals("OUTPUT")) {
                    //查询执行结果的地址
                    JCLInfo res_jclinfo = new JCLInfo();
                    String JESMSGLG_url = urlOverHttps + "/files/2/records";
                    String JESJCL_url = urlOverHttps + "/files/3/records";
                    String JESYSMSG_url = urlOverHttps + "/files/4/records";
                    String SYSPRINT_url = urlOverHttps + "/files/102/records";
                    ResponseEntity<String> res_JESMSGLG = new RestTemplate(requestFactory).exchange(JESMSGLG_url, HttpMethod.GET, requestQur, String.class);
                    res_jclinfo.setJESMSGLG(res_JESMSGLG.getBody());
                    ResponseEntity<String> res_JESJCL = new RestTemplate(requestFactory).exchange(JESJCL_url, HttpMethod.GET, requestQur, String.class);
                    res_jclinfo.setJESJCL(res_JESJCL.getBody());
                    ResponseEntity<String> res_JESYSMSG = new RestTemplate(requestFactory).exchange(JESYSMSG_url, HttpMethod.GET, requestQur, String.class);
                    res_jclinfo.setJESYSMSG(res_JESYSMSG.getBody());
//                    ResponseEntity<String> res_SYSPRINT = new RestTemplate(requestFactory).exchange(SYSPRINT_url, HttpMethod.GET, requestQur, String.class);
//                    res_jclinfo.setSYSPRINT(res_SYSPRINT.getBody());
                    return new ResponseEntity<JCLInfo>(res_jclinfo, HttpStatus.OK);
                }
            }
            //超时
            return new ResponseEntity("time out", HttpStatus.valueOf(202));
        }
    }

    //ismf 13.1
    @Deprecated
    @CrossOrigin(origins = "*", allowCredentials = "true")
    @RequestMapping(value = "/sms/ismf/13/1", method = RequestMethod.POST)
    public ResponseEntity<JCLInfo> subJob131(@RequestBody Map<String, String> map, HttpSession session) {
        Object ZOSMF_JSESSIONID = session.getAttribute("ZOSMF_JSESSIONID");
        Object ZOSMF_LtpaToken2 = session.getAttribute("ZOSMF_LtpaToken2");
        Object ZOSMF_Address = session.getAttribute("ZOSMF_Address");

        if (ZOSMF_JSESSIONID == null || ZOSMF_LtpaToken2 == null || ZOSMF_Address == null) {
            return new ResponseEntity("unauthorized", HttpStatus.valueOf(401));
        } else {
            CloseableHttpClient httpClient = SslUtil.SslHttpClientBuild();
            HttpComponentsClientHttpRequestFactory requestFactory
                    = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setHttpClient(httpClient);
            //jcljob提交地址
            String jclAddress = ZOSMF_Address.toString();
            String urlOverHttps = "https://" + jclAddress + "/zosmf/restjobs/jobs";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.add("Cookie", ZOSMF_JSESSIONID.toString() + ";" + ZOSMF_LtpaToken2);
            //body
            //获取参数
            String id = map.get("id");
            String Scds = map.get("scds");
            String memName = map.get("memName");
            String listName = map.get("listName");
            String testName = map.get("testName");

            StringBuffer sb = new StringBuffer();
            sb.append("//ST010113 JOB (ACCT),'ST010',MSGCLASS=H,");
            sb.append("//      NOTIFY=ST010,CLASS=A,MSGLEVEL=(1,1),TIME=(0,10)\n");
            sb.append("//MYLIB JCLLIB ORDER=SYS1.SACBCNTL\n");
            sb.append("//TRANSLAT EXEC ACBJBAOB,\n");
            sb.append("//         PLIB1='SYS1.DGTPLIB',\n");
            sb.append("//         TABL2=ST010.TEST.ISPTABL\n");
            sb.append("//SYSTSIN  DD *\n");
            sb.append("PROFILE PREFIX(ST010)\n");
            sb.append("ISPSTART CMD(ACBQBAIA +\n");
            sb.append("SCDS('ST010.SMS.SCDS') +\n");
            sb.append("TESTBED('ST010.SMS.TEST') MEMBER(CASE1) +\n");
            sb.append("DC(Y) SC(Y) MC(Y) SG(Y) +\n");
            sb.append("LISTNAME(NEW.LISTING)) +\n");
            sb.append("NEWAPPL(DGT) BATSCRW(132) BATSCRD(27) BREDIMAX(3) BDISPMAX(99999999)\n");
            sb.append("/*");

            //提交jcl的request
            HttpEntity<String> requestSub = new HttpEntity<>(sb.toString(), headers);
            //响应内容
            ResponseEntity<JobInfo> responseSub = new RestTemplate(requestFactory).exchange(urlOverHttps, HttpMethod.PUT, requestSub, JobInfo.class);

            //query job's status
            for (int i = 0; i < 10; i++) {
                try {
                    Thread.currentThread().sleep(1000);//
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                //查询执行状态的地址
                urlOverHttps = "https://" + jclAddress + "/zosmf/restjobs/jobs/" + responseSub.getBody().getJobName() + "/" + responseSub.getBody().getJobId();
                //查询结果
                HttpEntity<String> requestQur = new HttpEntity<>(headers);
                ResponseEntity<JobInfo> responseQur = new RestTemplate(requestFactory).exchange(urlOverHttps, HttpMethod.GET, requestQur, JobInfo.class);
                //判断作业状态
                if (responseQur.getBody().getStatus().equals("OUTPUT")) {
                    //查询执行结果的地址
                    JCLInfo res_jclinfo = new JCLInfo();
                    String JESMSGLG_url = urlOverHttps + "/files/2/records";
                    String JESJCL_url = urlOverHttps + "/files/3/records";
                    String JESYSMSG_url = urlOverHttps + "/files/4/records";
                    String SYSPRINT_url = urlOverHttps + "/files/102/records";
                    ResponseEntity<String> res_JESMSGLG = new RestTemplate(requestFactory).exchange(JESMSGLG_url, HttpMethod.GET, requestQur, String.class);
                    res_jclinfo.setJESMSGLG(res_JESMSGLG.getBody());
                    ResponseEntity<String> res_JESJCL = new RestTemplate(requestFactory).exchange(JESJCL_url, HttpMethod.GET, requestQur, String.class);
                    res_jclinfo.setJESJCL(res_JESJCL.getBody());
                    ResponseEntity<String> res_JESYSMSG = new RestTemplate(requestFactory).exchange(JESYSMSG_url, HttpMethod.GET, requestQur, String.class);
                    res_jclinfo.setJESYSMSG(res_JESYSMSG.getBody());
//                    ResponseEntity<String> res_SYSPRINT = new RestTemplate(requestFactory).exchange(SYSPRINT_url, HttpMethod.GET, requestQur, String.class);
//                    res_jclinfo.setSYSPRINT(res_SYSPRINT.getBody());
                    return new ResponseEntity<JCLInfo>(res_jclinfo, HttpStatus.OK);
                }
            }
            //超时
            return new ResponseEntity("time out", HttpStatus.valueOf(202));
        }
    }

    /**
     * Display base configuration of a SCDS
     */
    @CrossOrigin(origins = "*", allowCredentials = "true")
    @RequestMapping(value = "/sms/base-configuration/{scds}", method = RequestMethod.GET)
    public ResponseEntity<String> getBaseConfig(@PathVariable String scds, HttpSession session) {
        if (AuthUtil.notLogin(session)) {
            return ResponseEntity.status(401).body(null);
        }
        String res = ss.getBaseConfig(session, scds);
        if (res == null || res.equals("")) {
            res = "Can not get base configuration of " + scds +
                    ".\n Or time out.";
        }
        return ResponseEntity.ok(res);
    }

    /**
     * Create base configuration of a SCDS
     */
    @CrossOrigin(origins = "*", allowCredentials = "true")
    @RequestMapping(value = "/sms/base-configuration", method = RequestMethod.POST)
    public ResponseEntity<String> createBaseConfig(@RequestBody BaseConfiguration config, HttpSession session) {
        if (AuthUtil.notLogin(session)) {
            return ResponseEntity.status(401).body(null);
        }
        String res = ss.createBaseConfig(session, config);
        if (res == null || res.equals("")) {
            res = "Can not create base configuration of " + config.getScds() +
                    ".\n Or time out.";
        }
        return ResponseEntity.ok(res);
    }

    /**
     * Alter base configuration of a SCDS
     */
    @CrossOrigin(origins = "*", allowCredentials = "true")
    @RequestMapping(value = "/sms/base-configuration", method = RequestMethod.PUT)
    public ResponseEntity<String> alterBaseConfig(@RequestBody BaseConfiguration config, HttpSession session) {
        if (AuthUtil.notLogin(session)) {
            return ResponseEntity.status(401).body(null);
        }
        String res = ss.alterBaseConfig(session, config);
        if (res == null || res.equals("")) {
            res = "Can not alter base configuration of " + config.getScds() +
                    ".\n Or time out.";
        }
        return ResponseEntity.ok(res);
    }

    /**
     * Create data class of a SCDS
     */
    @CrossOrigin(origins = "*", allowCredentials = "true")
    @RequestMapping(value = "/sms/data-class", method = RequestMethod.POST)
    public ResponseEntity<String> createDataClass(@RequestBody DataClass dataClass, HttpSession session) {
        if (AuthUtil.notLogin(session)) {
            return ResponseEntity.status(401).body(null);
        }
        String res = ss.createDataClass(session, dataClass);
        if (res == null || res.equals("")) {
            res = "Can not create data class of " + dataClass.getScds() +
                    ".\n Or time out.";
        }
        return ResponseEntity.ok(res);
    }

    /**
     * Create storage class
     */
    @CrossOrigin(origins = "*", allowCredentials = "true")
    @RequestMapping(value = "/sms/storage-class", method = RequestMethod.POST)
    public ResponseEntity<String> createStorageClass(@RequestBody StorageClass storageClass, HttpSession session) {
        if (AuthUtil.notLogin(session)) {
            return ResponseEntity.status(401).body(null);
        }
        String res = ss.createStorageClass(session, storageClass);
        if (res == null || res.equals("")) {
            res = "Can not create storage class of " + storageClass.getScds() +
                    ".\n Or time out.";
        }
        return ResponseEntity.ok(res);
    }

    /**
     * Create management class
     */
    @CrossOrigin(origins = "*", allowCredentials = "true")
    @RequestMapping(value = "/sms/management-class", method = RequestMethod.POST)
    public ResponseEntity<String> createManagementClass(@RequestBody ManagementClass managementClass, HttpSession session) {
        if (AuthUtil.notLogin(session)) {
            return ResponseEntity.status(401).body(null);
        }
        String res = ss.createManagementClass(session, managementClass);
        if (res == null || res.equals("")) {
            res = "Can not create management class of " + managementClass.getScds() +
                    ".\n Or time out.";
        }
        return ResponseEntity.ok(res);
    }

    /**
     * Create storage group of pool type
     */
    @CrossOrigin(origins = "*", allowCredentials = "true")
    @RequestMapping(value = "/sms/storage-group/pool", method = RequestMethod.POST)
    public ResponseEntity<String> createPoolStorageGroup(@RequestBody PoolStorageGroup poolStorageGroup, HttpSession session) {
        if (AuthUtil.notLogin(session)) {
            return ResponseEntity.status(401).body(null);
        }
        String res = ss.createPoolStorageGroup(session, poolStorageGroup);
        if (res == null || res.equals("")) {
            res = "Can not create pool storage group of " + poolStorageGroup.getScds() +
                    ".\n Or time out.";
        }
        return ResponseEntity.ok(res);
    }
}


