package fun.dodo.verticle.loghubClient;

import com.aliyun.openservices.log.Client;
import com.aliyun.openservices.log.common.*;
import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.request.*;
import com.aliyun.openservices.log.response.BatchGetLogResponse;
import com.aliyun.openservices.log.response.GetCursorResponse;
import com.aliyun.openservices.log.response.GetHistogramsResponse;
import com.aliyun.openservices.log.response.GetLogsResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.logging.SimpleFormatter;

public class Sdksample {
    public static void main(String args[]) throws LogException, InterruptedException {
        String endpoint = "http://dodo-demo.cn-beijing.log.aliyuncs.com"; // 选择与上面步骤创建 project 所属区域匹配的
        // Endpoint
        String accessKeyId = "LTAICWWsWX6u2qJ9"; // 使用您的阿里云访问密钥 AccessKeyId
        String accessKeySecret = "1YsL1rKknuAwoRDSskG1VwBELgFuLc"; // 使用您的阿里云访问密钥
        // AccessKeySecret
        String project = "dodo-demo"; // 上面步骤创建的项目名称
        String logstore = "application-metrics"; // 上面步骤创建的日志库名称
        // 构建一个客户端实例
        Client client = new Client(endpoint, accessKeyId, accessKeySecret);
        // 列出当前 project 下的所有日志库名称
        int offset = 0;
        int size = 100;
        String logStoreSubName = "";
        ListLogStoresRequest req1 = new ListLogStoresRequest(project, offset, size, logStoreSubName);
        ArrayList<String> logStores = client.ListLogStores(req1).GetLogStores();
        System.out.println("ListLogs:" + logStores.toString() + "\n");

        // 写入日志
        String topic = "";
        String source = "";
//        // 连续发送 10 个数据包，每个数据包有 10 条日志
//        for (int i = 0; i < 10; i++) {
//            Vector<LogItem> logGroup = new Vector<LogItem>();
//            for (int j = 0; j < 10; j++) {
//                LogItem logItem = new LogItem((int) (new Date().getTime() / 1000));
//                logItem.PushBack("index"+String.valueOf(j), String.valueOf(i * 10 + j));
//                logGroup.add(logItem);
//            }
//            PutLogsRequest req2 = new PutLogsRequest(project, logstore, topic, source, logGroup);
//            client.PutLogs(req2);
//            /*
//             * 发送的时候也可以指定将数据发送至有一个特定的 shard，只要设置 shard 的 hashkey，则数据会写入包含该
//             * hashkey 的 range 所对应的 shard，具体 API 参考以下接口： public PutLogsResponse
//             * PutLogs( String project, String logStore, String topic,
//             * List<LogItem> logItems, String source, String shardHash // 根据
//             * hashkey 确定写入 shard，hashkey 可以是 MD5(ip) 或 MD5(id) 等 ) throws
//             * LogException;
//             */
//        }

        // 把 0 号 shard 中，最近 1 分钟写入的数据都读取出来。
        int shard_id = 0;
        long curTimeInSec = System.currentTimeMillis() / 1000;
        //
        GetCursorResponse cursorRes = client.GetCursor(project, logstore, shard_id, curTimeInSec - 600);
        String beginCursor = cursorRes.GetCursor();
        cursorRes = client.GetCursor(project, logstore, shard_id, Consts.CursorMode.END);
        String endCursor = cursorRes.GetCursor();
        String curCursor = beginCursor;
        while (curCursor.equals(endCursor) == false) {
            int loggroup_count = 2; // 每次读取两个 loggroup
            BatchGetLogResponse logDataRes = client.BatchGetLog(project, logstore, shard_id, loggroup_count, curCursor,
                    endCursor);
            // 读取LogGroup的List
            List<LogGroupData> logGroups = logDataRes.GetLogGroups();
            for(LogGroupData logGroup: logGroups){
                FastLogGroup flg = logGroup.GetFastLogGroup();
                System.out.println(String.format("\tcategory\t:\t%s\n\tsource\t:\t%s\n\ttopic\t:\t%s\n\tmachineUUID\t:\t%s",
                        flg.getCategory(), flg.getSource(), flg.getTopic(), flg.getMachineUUID()));
                System.out.println("Tags");
                for (int tagIdx = 0; tagIdx < flg.getLogTagsCount(); ++tagIdx) {
                    FastLogTag logtag = flg.getLogTags(tagIdx);
                    System.out.println(String.format("\t%s\t:\t%s", logtag.getKey(), logtag.getValue()));
                }
                for (int lIdx = 0; lIdx < flg.getLogsCount(); ++lIdx) {
                    FastLog log = flg.getLogs(lIdx);
                    System.out.println("--------\nLog: " + lIdx + ", time: " + log.getTime() + ", GetContentCount: " + log.getContentsCount());
                    for (int cIdx = 0; cIdx < log.getContentsCount(); ++cIdx) {
                        FastLogContent content = log.getContents(cIdx);
                        System.out.println(content.getKey() + "\t:\t" + content.getValue());
                    }
                }
            }
            String next_cursor = logDataRes.GetNextCursor();
            System.out.println("The Next cursor:" + next_cursor);
            curCursor = next_cursor;
        }
        // ！！！重要提示 : 只有打开索引功能，才能调用以下接口 ！！！
        // 等待 1 分钟让日志可查询
//        try {
//            Thread.sleep(60 * 1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        // 查询日志分布情况
        String query = "";
        int from = (int) (new Date().getTime() / 1000 - 30000);
        int to = (int) (new Date().getTime() / 1000);
        GetHistogramsResponse res3 = null;
        while (true) {
            GetHistogramsRequest req3 = new GetHistogramsRequest(project, logstore, topic, query, from, to);
            res3 = client.GetHistograms(req3);
            if (res3 != null && res3.IsCompleted()) // IsCompleted() 返回
            // true，表示查询结果是准确的，如果返回
            // false，则重复查询
            {
                break;
            }
            Thread.sleep(200);
        }
        System.out.println("Total count of logs is " + res3.GetTotalCount());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        for (Histogram ht : res3.GetHistograms()) {
            System.out.printf("from %s, to %s, count %d.\n", formatter.format(new Date(ht.GetFrom()*1000l)), formatter.format(new Date(ht.GetTo()*1000l)), ht.GetCount());
        }

        // 查询日志数据
        long total_log_lines = res3.GetTotalCount();
        int log_offset = 0;
        int log_line = 10;//log_line 最大值为100，每次获取100行数据。若需要读取更多数据，请使用offset翻页。offset和lines只对关键字查询有效，若使用SQL查询，则无效。在SQL查询中返回更多数据，请使用limit语法。
        GetLogsResponse res4 = null;
        while (log_offset <= total_log_lines) {
            // 对于每个 log offset,一次读取 10 行 log，如果读取失败，最多重复读取 3 次。
            for (int retry_time = 0; retry_time < 3; retry_time++) {
                GetLogsRequest req4 = new GetLogsRequest(project, logstore, from, to, topic, query, log_offset,
                        log_line, false);
                res4 = client.GetLogs(req4);
                if (res4 != null && res4.IsCompleted()) {
                    break;
                }
                Thread.sleep(200);
            }
//            System.out.println("Read log count:" + String.valueOf(res4.GetCount()));
            log_offset += log_line;
        }

        for (QueriedLog ht : res4.GetLogs()) {
            System.out.printf("GetLogContents %s.\n", ht.GetLogItem().GetLogContents());
        }

        //打开分析功能,只有打开分析功能，才能使用SQL 功能。 可以在控制台开通分析功能，也可以使用SDK开启分析功能
//        IndexKeys indexKeys = new IndexKeys();
//        ArrayList<String> tokens = new ArrayList<String>();
//        tokens.add(",");
//        tokens.add(".");
//        tokens.add("#");
//        IndexKey keyContent = new IndexKey(tokens,false,"text");
//        indexKeys.AddKey("index0",keyContent);
//        keyContent = new IndexKey(new ArrayList<String>(),false,"long");
//        indexKeys.AddKey("index1",keyContent);
//        keyContent = new IndexKey(new ArrayList<String>(),false,"double");
//        indexKeys.AddKey("index2",keyContent);
//        IndexLine indexLine = new IndexLine(new ArrayList<String>(),false);
//        Index index = new Index(7,indexKeys,indexLine);
//        CreateIndexRequest createIndexRequest = new CreateIndexRequest(project,logstore,index);
//        client.CreateIndex(createIndexRequest);
        //使用分析功能
        GetLogsRequest req4 = new GetLogsRequest(project, logstore, from, to, "", "* | select page,count(*) as count group by page order by count desc limit 10");
        GetLogsResponse res5 = client.GetLogs(req4);
        if(res5 != null && res5.IsCompleted()){
            for (QueriedLog log : res5.GetLogs()){
                LogItem item = log.GetLogItem();
                for(LogContent content : item.GetLogContents()){
                    System.out.print(content.GetKey()+":"+content.GetValue());
                }
                System.out.println();
            }
        }
    }
}
