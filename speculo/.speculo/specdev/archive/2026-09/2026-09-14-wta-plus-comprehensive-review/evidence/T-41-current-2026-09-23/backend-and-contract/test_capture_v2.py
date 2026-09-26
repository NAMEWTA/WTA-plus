import copy,importlib.util,json,pathlib,unittest
ROOT=pathlib.Path('/srv/WTA-plus');P=pathlib.Path('/tmp/wta-t41/capture-live-openapi-v2.py')
s=importlib.util.spec_from_file_location('capture_v2',P);m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
OLD=json.loads((ROOT/'frontend/packages/api-contracts/openapi/revisions/6dcd90b63abedf4d897c710e0b73709c7d202b18bc993b2ccc24a353d70fa1ce/source.json').read_bytes())
LIVE=json.loads(pathlib.Path('/tmp/wta-t41/openapi-live/7671ef3441f17602/source.json').read_bytes())
class CaptureMigration(unittest.TestCase):
 def check(self,live,old=OLD):return m.validate_full_openapi(json.dumps(live).encode(),old)
 def test_observed_live_migration_has_exact_one_retirement(self):
  result=self.check(LIVE);self.assertEqual(result['missing_schemas'],1);self.assertEqual(result['unapproved_missing_schemas'],0);self.assertEqual(len(result['intentional_schema_replacements']),1)
 def test_already_migrated_baseline_retains_positive_contract(self):
  result=self.check(LIVE,LIVE);self.assertEqual(result['missing_schemas'],0)
 def test_other_missing_schema_rejected(self):
  value=copy.deepcopy(LIVE);del value['components']['schemas']['RetryReceipt'];self.assertRaises(m.OpenApiLossError,self.check,value)
 def test_missing_old_path_rejected(self):
  value=copy.deepcopy(LIVE);del value['paths']['/notify/notification/{notificationId}/cancel'];self.assertRaises(m.OpenApiLossError,self.check,value)
 def test_missing_old_method_rejected(self):
  value=copy.deepcopy(LIVE);del value['paths']['/notify/notification/{notificationId}/retry']['post'];self.assertRaises(m.OpenApiLossError,self.check,value)
 def test_retired_schema_with_second_consumer_rejected(self):
  old=copy.deepcopy(OLD);old['x-unknown-consumer']={'$ref':'#/components/schemas/RListNotifyInboxMessageVo'};self.assertRaises(RuntimeError,self.check,LIVE,old)
 def test_retired_schema_with_wrong_original_items_rejected(self):
  old=copy.deepcopy(OLD);old['components']['schemas']['RListNotifyInboxMessageVo']['properties']['data']['items']={'type':'string'};self.assertRaises(RuntimeError,self.check,LIVE,old)
 def test_missing_global_unread_rejected(self):
  value=copy.deepcopy(LIVE);del value['components']['schemas']['NotifyInboxPageVo']['properties']['unreadTotal'];self.assertRaises(RuntimeError,self.check,value)
 def test_wrong_rows_projection_rejected(self):
  value=copy.deepcopy(LIVE);value['components']['schemas']['NotifyInboxPageVo']['properties']['rows']['items']={'type':'string'};self.assertRaises(RuntimeError,self.check,value)
 def test_detail_missing_rejected(self):
  value=copy.deepcopy(LIVE);del value['paths']['/notify/inbox/{messageId}'];self.assertRaises(RuntimeError,self.check,value)
 def test_untyped_page_query_rejected(self):
  value=copy.deepcopy(LIVE);value['paths']['/notify/inbox']['get']['parameters'][0]['schema']['type']='string';self.assertRaises(RuntimeError,self.check,value)
if __name__=='__main__':unittest.main()
