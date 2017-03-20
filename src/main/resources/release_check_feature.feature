Feature: Check the artifact in a release
  Scenario: Check the existence of required artifacts for Custom Reports Feature.
    Given The latest Release Package
    When installed
    Then the version of maven artifact "com.cisco.reports:reports-generator" should be "1.0.9"
    And the version of maven artifact "com.cisco.reports:reports-generator-api" should be greater than "1.0.8"

