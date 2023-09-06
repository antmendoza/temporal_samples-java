package io.jmpchase;

import java.util.Iterator;
import java.util.Map;

public class Main {

  public static void main(String[] args) {

    int a = 0;
    Map<String, String> map =
        Map.of(
            "key" + ++a, "value" + ++a,
            "key" + ++a, "value" + ++a,
            "key" + ++a, "value" + ++a,
            "key" + ++a, "value" + ++a,
            "key" + ++a, "value" + ++a);

    Iterator<Map.Entry<String, String>> iterator = map.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, String> entry = iterator.next();
      System.out.println(entry.getKey() + ":" + entry.getValue());
    }
  }
}
