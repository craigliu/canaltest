package com.rtbasia.canal.test;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.google.protobuf.InvalidProtocolBufferException;

import java.net.InetSocketAddress;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        CanalConnector connector = CanalConnectors.newSingleConnector(new InetSocketAddress("localhost", 11111),
                "example", "", "");

        connector.connect();
        connector.subscribe();
        connector.rollback();

        while (true) {
            Message message = connector.getWithoutAck(1000);

            long batchID = message.getId();
            int size = message.getEntries().size();

            if (batchID == -1 || size == 0) {
                System.out.println("没有数据, 等待...");

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    System.out.println("收到中断信号, 退出...");

                    break;
                }
            } else {
                System.out.println("-------------------------- 收到数据 -----------------------");
                printEntry(message.getEntries(), batchID);
            }

            connector.ack(batchID);
        }
    }

    private static void printEntry(List<CanalEntry.Entry> entries, long batchId) {
        for (CanalEntry.Entry entry : entries) {
            CanalEntry.Header header = entry.getHeader();
            String logFileName = header.getLogfileName();
            long offset = header.getLogfileOffset();

            CanalEntry.EntryType entryType = entry.getEntryType();

            System.out.println(String.format("logFileName: %s, offset %d, entry type: %s", logFileName, offset,
                    entryType.name()));

            if (entryType == CanalEntry.EntryType.ROWDATA) {
                try {
                    CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());

                    CanalEntry.EventType eventType = rowChange.getEventType();
                    String tableName = header.getTableName();
                    String schemaName = header.getSchemaName();

                    System.out.println(String.format("当前正在操作 %s.%s， Action= %s", schemaName, tableName, eventType));

                    System.out.println("sql: " + rowChange.getSql());

                    rowChange.getRowDatasList().forEach((rowData) -> {
                        // 获取更新之前的column情况
                        List<CanalEntry.Column> beforeColumns = rowData.getBeforeColumnsList();

                        // 获取更新之后的 column 情况
                        List<CanalEntry.Column> afterColumns = rowData.getAfterColumnsList();

                        // 当前执行的是 删除操作
                        if (eventType == CanalEntry.EventType.DELETE) {
                            printColumn(beforeColumns);
                        }

                        // 当前执行的是 插入操作
                        if (eventType == eventType.INSERT) {
                            printColumn(afterColumns);
                        }

                        // 当前执行的是 更新操作
                        if (eventType == eventType.UPDATE) {
                            printColumn(afterColumns);
                        }
                    });
                } catch (InvalidProtocolBufferException e) {
                    System.out.println("数据格式不正确, 抛弃掉这一条数据");
                }
            }
        }
    }

    public static void printColumn(List<CanalEntry.Column> columns) {
        columns.forEach((column) -> {
            String columnName = column.getName();
            String columnValue = column.getValue();
            String columnType = column.getMysqlType();
            boolean isUpdated = column.getUpdated();

            System.out.println(String.format("columnName=%s, columnValue=%s, columnType=%s, isUpdated=%s", columnName,
                    columnValue, columnType, isUpdated));
        });
    }
}
