package org.health.service.med;

import org.health.common.ResultCode;
import org.health.entity.med.DrugCatalog;
import org.health.mapper.med.DrugCatalogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 药品服务
 */
@Service
public class DrugService {

  @Autowired
  private DrugCatalogMapper drugCatalogMapper;

  /**
   * 搜索药品列表
   *
   * @param keyword 关键词
   * @param page    页码
   * @param size    每页大小
   * @return 药品列表
   */
  public DrugListVO searchDrugs(String keyword, Integer page, Integer size) {
    // 默认值
    if (page == null || page < 1) {
      page = 1;
    }
    if (size == null || size < 1) {
      size = 20;
    }

    int offset = (page - 1) * size;

    // 查询列表
    List<DrugCatalog> drugs = drugCatalogMapper.searchDrugs(keyword, offset, size);
    int total = drugCatalogMapper.countDrugs(keyword);

    DrugListVO vo = new DrugListVO();
    vo.setList(drugs.stream().map(drug -> {
      DrugVO drugVO = new DrugVO();
      drugVO.setId(drug.getId());
      drugVO.setName(drug.getName());
      drugVO.setTags(drug.getTags());
      return drugVO;
    }).collect(Collectors.toList()));
    vo.setPage(page);
    vo.setSize(size);
    vo.setTotal(total);

    return vo;
  }

  /**
   * 获取药品详情
   *
   * @param id 药品ID
   * @return 药品详情
   */
  public DrugDetailVO getDrugDetail(Long id) {
    DrugCatalog drug = drugCatalogMapper.selectById(id);
    if (drug == null) {
      throw new RuntimeException(ResultCode.NOT_FOUND.getMsg());
    }

    DrugDetailVO vo = new DrugDetailVO();
    vo.setId(drug.getId());
    vo.setName(drug.getName());
    vo.setCommonNames(drug.getCommonNames());
    vo.setIntro(drug.getIntro());
    vo.setUsage(drug.getUsage());
    vo.setWarnings(drug.getWarnings());
    vo.setDisclaimer(drug.getDisclaimer());

    return vo;
  }

  /**
   * 药品列表视图对象
   */
  public static class DrugListVO {
    private List<DrugVO> list;
    private Integer page;
    private Integer size;
    private Integer total;

    public List<DrugVO> getList() {
      return list;
    }

    public void setList(List<DrugVO> list) {
      this.list = list;
    }

    public Integer getPage() {
      return page;
    }

    public void setPage(Integer page) {
      this.page = page;
    }

    public Integer getSize() {
      return size;
    }

    public void setSize(Integer size) {
      this.size = size;
    }

    public Integer getTotal() {
      return total;
    }

    public void setTotal(Integer total) {
      this.total = total;
    }
  }

  /**
   * 药品视图对象（列表）
   */
  public static class DrugVO {
    private Long id;
    private String name;
    private List<String> tags;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public List<String> getTags() {
      return tags;
    }

    public void setTags(List<String> tags) {
      this.tags = tags;
    }
  }

  /**
   * 药品详情视图对象
   */
  public static class DrugDetailVO {
    private Long id;
    private String name;
    private List<String> commonNames;
    private String intro;
    private String usage;
    private String warnings;
    private String disclaimer;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public List<String> getCommonNames() {
      return commonNames;
    }

    public void setCommonNames(List<String> commonNames) {
      this.commonNames = commonNames;
    }

    public String getIntro() {
      return intro;
    }

    public void setIntro(String intro) {
      this.intro = intro;
    }

    public String getUsage() {
      return usage;
    }

    public void setUsage(String usage) {
      this.usage = usage;
    }

    public String getWarnings() {
      return warnings;
    }

    public void setWarnings(String warnings) {
      this.warnings = warnings;
    }

    public String getDisclaimer() {
      return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
      this.disclaimer = disclaimer;
    }
  }
}
