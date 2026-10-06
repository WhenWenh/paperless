import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TagsComponent } from './tags.component';

describe('TagsComponent', () => {
  let component: TagsComponent;
  let fixture: ComponentFixture<TagsComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TagsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(TagsComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
    httpMock.expectOne(req => req.method === 'GET').flush([
      { id: '1', name: 'Rechnungen' }
    ]);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create and load tags', () => {
    expect(component).toBeTruthy();
    expect(component.tags().length).toBe(1);
  });

  it('should reject an empty tag name before sending', () => {
    component.newTagName.set('   ');
    component.addTag();

    expect(component.validationError()).toBe('Tag name is required.');
    httpMock.expectNone(req => req.method === 'POST');
  });

  it('should reject a duplicate name before sending', () => {
    component.newTagName.set('rechnungen');
    component.addTag();

    expect(component.validationError()).toContain('already exists');
    httpMock.expectNone(req => req.method === 'POST');
  });

  it('should add a created tag to the list', () => {
    component.newTagName.set('Verträge');
    component.addTag();

    httpMock.expectOne(req => req.method === 'POST')
      .flush({ id: '2', name: 'Verträge' });

    expect(component.tags().map(tag => tag.name))
      .toEqual(['Rechnungen', 'Verträge']);
    expect(component.newTagName()).toBe('');
  });

  it('should show the server message when the name already exists', () => {
    component.newTagName.set('Neu');
    component.addTag();

    httpMock.expectOne(req => req.method === 'POST').flush(
      { message: 'Tag already exists: Neu' },
      { status: 409, statusText: 'Conflict' }
    );

    expect(component.error()).toBe('Tag already exists: Neu');
  });

  it('should remove a deleted tag from the list', () => {
    component.deleteTag({ id: '1', name: 'Rechnungen' });

    httpMock.expectOne(req => req.method === 'DELETE').flush(null);

    expect(component.tags()).toEqual([]);
  });

  it('should show the server message when the tag is still in use', () => {
    component.deleteTag({ id: '1', name: 'Rechnungen' });

    httpMock.expectOne(req => req.method === 'DELETE').flush(
      { message: 'Tag 1 is still assigned to 3 document(s)' },
      { status: 409, statusText: 'Conflict' }
    );

    expect(component.error()).toBe('Tag 1 is still assigned to 3 document(s)');
    expect(component.tags().length).toBe(1);
  });
});
